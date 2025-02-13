package com.supalle;

import com.supalle.nice.rpc.NiceRPC;

@NiceRPC
public interface StudentService {

    Student<Void> getStudent(int id);

    Student<Void> getStudent2(int id);

}
