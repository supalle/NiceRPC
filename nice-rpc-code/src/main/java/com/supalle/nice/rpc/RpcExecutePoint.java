package com.supalle.nice.rpc;

public interface RpcExecutePoint {

    RpcMethod getRpcMethod();

    Object getTarget();

    Object[] getArgs();

}
