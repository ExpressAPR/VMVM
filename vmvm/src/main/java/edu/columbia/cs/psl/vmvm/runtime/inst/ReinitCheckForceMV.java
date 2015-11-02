package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.FrameNode;

import edu.columbia.cs.psl.vmvm.runtime.ClassState;
import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;

public class ReinitCheckForceMV extends MethodVisitor {
	private AnalyzerAdapter an;
	private boolean isStaticMethod;
	private String owner;
	private boolean needOldLdc;
	private String name;
	private boolean skipFrames;
	private boolean hasAnyChecks;
	private boolean doOpt = false;
	public ReinitCheckForceMV(MethodVisitor mv, AnalyzerAdapter analyzer, String owner, String name, boolean isStaticMethod, boolean needOldLdc, boolean skipFrames) {
		super(Opcodes.ASM5, mv);
		this.isStaticMethod = isStaticMethod;
		this.owner = owner;
		this.name = name;
		this.an = analyzer;
		this.needOldLdc = needOldLdc;
		this.skipFrames = skipFrames;
	}

	public static Object[] removeLongsDoubleTopVal(List<?> in) {
		ArrayList<Object> ret = new ArrayList<Object>();
		boolean lastWas2Word = false;
		for (Object n : in) {
			if (n == Opcodes.TOP && lastWas2Word) {
				//nop
			} else
				ret.add(n);
			if (n == Opcodes.DOUBLE || n == Opcodes.LONG)
				lastWas2Word = true;
			else
				lastWas2Word = false;
		}
		return ret.toArray();
	}

	public FrameNode getCurrentFrame() {
		if (skipFrames)
			return null;
		if (an.locals == null || an.stack == null)
			throw new IllegalArgumentException("In " + owner + "." + name);
		Object[] locals = removeLongsDoubleTopVal(an.locals);
		Object[] stack = removeLongsDoubleTopVal(an.stack);
		FrameNode ret = new FrameNode(Opcodes.F_NEW, locals.length, locals, stack.length, stack);
		return ret;
	}

	public void println(String toPrint) {
		visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		visitLdcInsn(toPrint + " : ");
		super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);

		visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
		super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		
		if ((opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) && !VMVMClassFileTransformer.isIgnoredClass(owner) //&& !owner.equals(this.owner)
				&& !name.equals(Constants.VMVM_NEEDS_RESET)
						&& !desc.equals("L"+MutableInstance.INTERNAL_NAME+";")
						&& !name.equals("$$VMVM_RESETTER")
						&& !name.equals(Constants.VMVM_RESET_IN_PROGRESS)
		&& (!doOpt || Reinitializer.classesNotYetReinitialized.contains(owner.replace("/", ".")))) {
			hasAnyChecks = true;
			if(opcode == Opcodes.GETSTATIC)
			{
				super.visitFieldInsn(Opcodes.GETSTATIC, owner, Constants.VMVM_RESET_SUFFIX, "L"+owner+Constants.VMVM_RESET_SUFFIX+";");
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner+Constants.VMVM_RESET_SUFFIX, "get"+name, "()"+desc, false);
			}
			else
			{
				super.visitFieldInsn(Opcodes.GETSTATIC, owner, Constants.VMVM_RESET_SUFFIX, "L"+owner+Constants.VMVM_RESET_SUFFIX+";");
				Type t = Type.getType(desc);
				if(t.getSize() == 1)
				{
					super.visitInsn(Opcodes.SWAP);
				}
				else
				{
					super.visitInsn(Opcodes.DUP_X2);
					super.visitInsn(Opcodes.POP);
				}
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner+Constants.VMVM_RESET_SUFFIX, "set"+name, "("+desc+")V", false);
			}
		}
		else
			super.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitInsn(int opcode) {
		if (hasAnyChecks && !name.equals("<clinit>") && !name.equals("__vmvmReClinit"))
			switch (opcode) {
			case Opcodes.RETURN:
			case Opcodes.IRETURN:
			case Opcodes.FRETURN:
			case Opcodes.ARETURN:
			case Opcodes.LRETURN:
			case Opcodes.DRETURN:
				if (VMVMClassFileTransformer.ALWAYS_REOPT) {
					FrameNode fn = getCurrentFrame();
					super.visitVarInsn(Opcodes.ILOAD, tmpLVidx);
					Label done = new Label();
					super.visitJumpInsn(Opcodes.IFEQ, done);
					if (needOldLdc) {
						super.visitLdcInsn(this.owner.replace("/", "."));
						super.visitInsn(Opcodes.ICONST_0);
						super.visitLdcInsn(this.owner.replace("/", "."));
						super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
						super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
						super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
					} else
						super.visitLdcInsn(Type.getObjectType(this.owner));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "reopt", "(Ljava/lang/Class;)V", false);
					super.visitLabel(done);
					if (!skipFrames)
						fn.accept(this);
				}
				break;
			}
		super.visitInsn(opcode);
	}

	int tmpLVidx;

	@Override
	public void visitCode() {
		super.visitCode();

		if ((name.equals("<init>") || (isStaticMethod && !(name.equals("<clinit>") || name.equals("__vmvmReClinit") || name.equals("_vmvmReinitFieldCheck")))) && !doOpt) {
			Label ok = new Label();
			FrameNode fn = getCurrentFrame();
			super.visitFieldInsn(Opcodes.GETSTATIC, owner, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
			if (owner.equals("org/apache/log4j/Level")) { //XXX todo why was this necessary on nutch!?
				super.visitJumpInsn(Opcodes.IFNULL, ok);
				super.visitFieldInsn(Opcodes.GETSTATIC, owner, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
			}
			super.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
			super.visitJumpInsn(Opcodes.IFEQ, ok);
			if (VMVMClassFileTransformer.ALWAYS_REOPT) {
				hasAnyChecks = true;
				super.visitInsn(Opcodes.ICONST_1);
				super.visitVarInsn(Opcodes.ISTORE, tmpLVidx);
			}
			super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "__vmvmReClinit", "()V", false);
			super.visitLabel(ok);
			if (!skipFrames) {
				fn.accept(this);
				super.visitInsn(Opcodes.NOP);
			}
		}

	}

}
