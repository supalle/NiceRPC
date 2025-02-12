package com.supalle.nice.rpc;

public interface RpcEngine {

    <T> T call(RpcExecutePoint rpcExecutePoint);

}
