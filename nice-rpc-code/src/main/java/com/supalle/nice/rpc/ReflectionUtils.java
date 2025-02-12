package com.supalle.nice.rpc;

import java.lang.reflect.Method;

public class ReflectionUtils {

    public static <T> Method getMethod(Class<T> methodClass, String methodName, Class<?>... parameterTypes) {
        try {
            return methodClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
