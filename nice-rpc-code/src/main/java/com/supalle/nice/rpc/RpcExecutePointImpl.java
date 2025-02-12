package com.supalle.nice.rpc;

public class RpcExecutePointImpl implements RpcExecutePoint {

    private final RpcMethod rpcMethod;
    private final Object target;
    private final Object[] args;
    private final RpcExecuteContext context;

    public RpcExecutePointImpl(RpcMethod rpcMethod, Object target, Object[] args, RpcExecuteContext context) {
        this.rpcMethod = rpcMethod;
        this.target = target;
        this.args = args;
        this.context = context;
    }

    @Override
    public RpcMethod getRpcMethod() {
        return this.rpcMethod;
    }

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public Object[] getArgs() {
        return this.args;
    }

    @Override
    public RpcExecuteContext getContext() {
        return context;
    }

}
