package com.supalle;

import lombok.Data;

@Data
public class Student<T> {
    private String name;
    private int age;
}