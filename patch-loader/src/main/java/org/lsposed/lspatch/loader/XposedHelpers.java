package org.lsposed.lspatch.loader;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Simple XposedHelpers replacement for LSPatch compatibility
 * This provides basic reflection functionality without full Xposed framework
 */
public class XposedHelpers {
    private static final String TAG = "LSPatch-XposedHelpers";
    
    /**
     * Get object field value
     */
    public static Object getObjectField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            Log.w(TAG, "Error getting field " + fieldName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set object field value
     */
    public static void setObjectField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            Log.w(TAG, "Error setting field " + fieldName + ": " + e.getMessage());
        }
    }
    
    /**
     * Find class by name
     */
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Class not found: " + className);
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
    
    /**
     * Call static method
     */
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            Method method = findMethodBestMatch(clazz, methodName, args);
            return method.invoke(null, args);
        } catch (Exception e) {
            Log.w(TAG, "Error calling static method " + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find method best match
     */
    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && 
                    method.getParameterTypes().length == args.length) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new NoSuchMethodException(methodName);
        } catch (Exception e) {
            throw new RuntimeException("Method not found: " + methodName, e);
        }
    }
}
