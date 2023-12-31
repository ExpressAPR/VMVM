package java.edu.columbia.cs.psl.vmvm.runtime;

import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;
import edu.columbia.cs.psl.vmvm.runtime.inst.Constants;
import org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class ReflectionWrapper {

	private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
		return clazz.getField(name);
	}
	private static Field access(Field f) {
		f.setAccessible(true);
		return f;
	}

	public static void putStaticField(Object value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putObject(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(boolean value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putBoolean(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(byte value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putByte(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(char value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putChar(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(int value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putInt(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(short value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putShort(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(long value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putLong(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(float value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putFloat(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}
	public static void putStaticField(double value, Class<?> clazz, String name) throws NoSuchFieldException {
		Field f = getField(clazz, name);
		Unsafe u = VMVMClassFileTransformer.getUnsafe();
		u.putDouble(u.staticFieldBase(f),u.staticFieldOffset(f), value);
	}


	public static Method[] getDeclaredMethods(Class<?> clazz) {
		Method[] r = clazz.getDeclaredMethods();
		if(VMVMClassFileTransformer.isIgnoredClass(clazz.getName()))
			return r;
		if(clazz.isInterface() || r.length == 0)
			return r;
		boolean found = false;
		for (Class c : clazz.getInterfaces())
			if (c.equals(VMVMInstrumented.class))
				found = true;
		if (!found)
			return r;
		Method[] ret = new Method[r.length - 1];
		int j = 0;
		for (int i = 0; i < r.length; i++) {
			if (r[i].getName().equals("__vmvmReClinit"))
				continue;
			ret[j] = r[i];
			j++;
		}
		return ret;
	}
	public static Class[] getInterfaces(Class<?> clazz) {
		Class[] ret = clazz.getInterfaces();
		ArrayList<Class> _ret = new ArrayList<Class>(ret.length);
		for (int i = 0; i < ret.length; i++) {
			if (ret[i] == VMVMInstrumented.class)
				continue;
			_ret.add(ret[i]);
		}
		return _ret.toArray(new Class[_ret.size()]);
	}
	public static Class<?> getType(Field f) throws IllegalArgumentException, IllegalAccessException
	{
		Class ret = f.getType();
		if(ret == MutableInstance.class)
			return ((MutableInstance)f.get(null)).getType();
		return ret;
	}
	public static Method[] getMethods(Class<?> clazz) {
		Method[] r = clazz.getMethods();
		if(VMVMClassFileTransformer.isIgnoredClass(clazz.getName()))
			return r;
		if(clazz.isInterface() || r.length == 0)
			return r;
		boolean found = false;
		for (Class c : clazz.getInterfaces())
			if (c.equals(VMVMInstrumented.class))
				found = true;
		if (!found)
			return r;
		Method[] ret = new Method[r.length - 1];
		int j = 0;
		for (int i = 0; i < r.length; i++) {
			if (r[i].getName().equals("__vmvmReClinit"))
				continue;
			ret[j] = r[i];
			j++;
		}
		return ret;
	}

	private static void checkInternalFinal(Field f, boolean acc) throws IllegalAccessException {
		// xmcp: check internal ENUM which means FINAL
		if(!acc && (f.getModifiers()&Opcodes.ACC_ENUM)!=0)
			throw new IllegalAccessException(String.format("Can not set FINAL %s to any value (vmvm)", f));
		// Lang 6 FieldUtilsTest: seems that STATIC FINAL fields cannot be written even with acc set to true
		if((f.getModifiers()&Opcodes.ACC_ENUM)!=0 && (f.getModifiers()&Opcodes.ACC_STATIC)!=0)
			throw new IllegalAccessException(String.format("Can not set STATIC FINAL %s to any value (vmvm)", f));
	}

	public static void set(Field f, Object owner, Object val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.set(owner, val);
		}
	}
	public static void setBoolean(Field f, Object owner, boolean val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setBoolean(owner, val);
		}
	}
	public static void setByte(Field f, Object owner, byte val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setByte(owner, val);
		}
	}
	public static void setChar(Field f, Object owner, char val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setChar(owner, val);
		}
	}
	public static void setDouble(Field f, Object owner, double val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setDouble(owner, val);
		}
	}
	public static void setInt(Field f, Object owner, int val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setInt(owner, val);
		}
	}
	public static void setFloat(Field f, Object owner, float val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setFloat(owner, val);
		}
	}
	public static void setLong(Field f, Object owner, long val) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		boolean acc = f.isAccessible();
		f.setAccessible(true);
		checkInternalFinal(f, acc);
		if (f.getType() == MutableInstance.class) {
			((MutableInstance) f.get(owner)).put(val);
		} else {
			if(!acc)
				f.setAccessible(false);
			f.setLong(owner, val);
		}
	}
	

	public static boolean getBoolean(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		f.setAccessible(true);
		if (f.getType() == MutableInstance.class)
			return ((Boolean) ((MutableInstance) f.get(owner)).get());
		return f.getBoolean(owner);
	}
	public static byte getByte(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		f.setAccessible(true);
		if (f.getType() == MutableInstance.class)
			return ((Byte) ((MutableInstance) f.get(owner)).get());
		return f.getByte(owner);
	}
	public static char getChar(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		f.setAccessible(true);
		if (f.getType() == MutableInstance.class)
			return ((Character) ((MutableInstance) f.get(owner)).get());
		return f.getChar(owner);
	}
	public static double getDouble(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		f.setAccessible(true);
		if (f.getType() == MutableInstance.class)
			return ((Double) ((MutableInstance) f.get(owner)).get());
		return f.getDouble(owner);
	}
	public static float getFloat(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		f.setAccessible(true);
		if (f.getType() == MutableInstance.class)
			return ((Float) ((MutableInstance) f.get(owner)).get());
		return f.getFloat(owner);
	}
	public static int getInt(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		if (f.getType() == MutableInstance.class)
			return ((Integer) ((MutableInstance) f.get(owner)).get());
		return f.getInt(owner);
	}
	public static long getLong(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		if (f.getType() == MutableInstance.class)
			return ((Long) ((MutableInstance) f.get(owner)).get());
		return f.getLong(owner);
	}
	public static Object get(Field f, Object owner) throws IllegalArgumentException, IllegalAccessException {
		tryToInit(f.getDeclaringClass());
		f.setAccessible(true);
		Object ret = f.get(owner);
		if (ret instanceof MutableInstance)
			return ((MutableInstance) ret).get();
		return ret;
	}

	public static Field[] getDeclaredFields(Class<?> clazz) {
		Field[] r = clazz.getDeclaredFields();
		ArrayList<Field> ret = new ArrayList<>(r.length);
		for (Field f : r) {
			if (!f.getName().startsWith("_vmvm") && !f.getName().endsWith("VMVM_CLASSES_TO_CHECK") && !f.getName().equals("VMVM_RESET_IN_PROGRESS") && !f.getName().equals("VMVM_NEEDS_RESET")
					&& !f.getName().equals("$$VMVM_RESETTER"))
				ret.add(f);
		}
		r = new Field[ret.size()];
		r = ret.toArray(r);
		return r;
	}

	public static Field[] getFields(Class<?> clazz) {
		Field[] r = clazz.getFields();
		ArrayList<Field> ret = new ArrayList<>(r.length);
		for (Field f : r) {
			if (!f.getName().startsWith("_vmvm") && !f.getName().endsWith("VMVM_CLASSES_TO_CHECK") && !f.getName().equals("VMVM_RESET_IN_PROGRESS") && !f.getName().equals("VMVM_NEEDS_RESET")
					&& !f.getName().equals("$$VMVM_RESETTER"))
				ret.add(f);
		}
		r = new Field[ret.size()];
		r = ret.toArray(r);
		return r;
	}

	public static Class<?> forName(String name, ClassLoader loader) throws ClassNotFoundException {
		Class<?> ret = Class.forName(name, true, loader);
		tryToInit(ret);
		return ret;
	}

	public static Class<?> preNewInstance(Class<?> clazz) throws InstantiationException, IllegalAccessException {
		tryToInit(clazz);
		return clazz;
	}

	public static Class<?> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		Class<?> ret = Class.forName(name, initialize, loader);
		if (initialize) {
			tryToInit(ret);
		}
		return ret;
	}

	public static Object invoke(Method m, Object owner, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (Modifier.isStatic(m.getModifiers()))
			tryToInit(m.getDeclaringClass());
		if (!m.isAccessible())
			m.setAccessible(true);
		return m.invoke(owner, args);
	}

	public static int getModifiers(Class<?> c) {
		//		VMVMClassFileTransformer.ensureInit();
		int ret = c.getModifiers();
		//		if(Instrumenter.finalClasses.contains(c.getName().replace(".", "/")))
		//			ret = ret | Modifier.FINAL;
		return ret;
	}

	public static int getModifiers(Method m) {
		//		VMVMClassFileTransformer.ensureInit();
		int ret = m.getModifiers();
		//		if(Instrumenter.finalMethods.contains(m.getDeclaringClass().getName().replace(".", "/") + "."+Type.getMethodDescriptor(m)))
		//			ret = ret | Modifier.FINAL;
		return ret;
	}

	public static int getModifiers(Field f) {
		//		VMVMClassFileTransformer.ensureInit();
		int ret = f.getModifiers();
		//		if(Instrumenter.finalFields.contains(f.getDeclaringClass().getName().replace(".", "/") + "."+Type.getDescriptor(f.getType())))
		//			ret = ret | Modifier.FINAL;
		return ret;
	}

	public static void tryToInit(Class<?> clazz) {
		//			if(inited.contains(clazz))
		//				return;
		//			inited.add(clazz);
		//			synchronized (clazz) {
		if(clazz.isArray())
			clazz = clazz.getComponentType();
		if(VMVMClassFileTransformer.isIgnoredClass(clazz.getName().replace('.','/')))
			return;
		try {
			boolean val = access(clazz.getField(Constants.VMVM_NEEDS_RESET)).getBoolean(null);
			if (val) {
				InterfaceReinitializer resetter = (InterfaceReinitializer) access(clazz.getField(Constants.VMVM_RESET_FIELD)).get(null);
				resetter.__vmvmReClinit();
			}
		} catch (Throwable ex) {
			if (!(ex instanceof NoSuchFieldException)) {
				ex.printStackTrace();
				System.err.println("Error on " + clazz);
			}

		}
		//		}
	}
}
