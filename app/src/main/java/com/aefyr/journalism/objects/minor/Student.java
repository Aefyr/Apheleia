package com.aefyr.journalism.objects.minor;


import com.aefyr.journalism.objects.major.PersonaInfo;

public class Student {
    String name;
    String id;
    PersonaInfo.Gender gender;
    String className;

    public String name() {
        return name;
    }

    public String id() {
        return id;
    }

    public PersonaInfo.Gender gender() {
        return gender;
    }

    public String className() {
        return className;
    }
}
