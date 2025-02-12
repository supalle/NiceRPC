package com.supalle.nice.rpc;

import java.lang.reflect.Method;

public class RpcMethodImpl implements RpcMethod {

    private final Method method;

    public RpcMethodImpl(Method method) {
        this.method = method;
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    public static <T> RpcMethod of(Class<T> declaringClass, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = declaringClass.getMethod(methodName, parameterTypes);
            return new RpcMethodImpl(method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
