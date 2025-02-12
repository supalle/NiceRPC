package com.supalle;

import com.supalle.nice.rpc.NiceRPC;

@NiceRPC
public interface StudentService {

    Student getStudent(int id);

}
