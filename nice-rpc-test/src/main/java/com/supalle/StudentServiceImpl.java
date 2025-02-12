package com.supalle;


public class StudentServiceImpl implements StudentService {

    private final NiceRpcEngine niceRpcEngine;

    public StudentServiceImpl(NiceRpcEngine niceRpcEngine) {
        this.niceRpcEngine = niceRpcEngine;
    }

    @Override
    public Student getStudent(int id) {
        return null;
    }

}
