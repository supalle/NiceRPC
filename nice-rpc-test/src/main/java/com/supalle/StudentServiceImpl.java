package com.supalle;


import com.supalle.nice.rpc.*;

public class StudentServiceImpl implements StudentService {

    private final RpcEngine rpcEngine;

    public StudentServiceImpl(RpcEngine rpcEngine) {
        this.rpcEngine = rpcEngine;
    }

    private final RpcMethod rpcMethod_getStudent = RpcMethodImpl.of(StudentServiceImpl.class, "getStudent", int.class);

    @Override
    public Student getStudent(int id) {
        RpcExecutePoint rpcExecutePoint = new RpcExecutePointImpl(rpcMethod_getStudent, this, new Object[]{id});
        return rpcEngine.call(rpcExecutePoint);
    }

}
