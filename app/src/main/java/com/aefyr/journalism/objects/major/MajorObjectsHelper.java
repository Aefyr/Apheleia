package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.major.PersonaInfo.Gender;
import com.aefyr.journalism.objects.major.PersonaInfo.Role;
import com.aefyr.journalism.objects.minor.ActualPeriod;
import com.aefyr.journalism.objects.minor.AmbigiousPeriod;
import com.aefyr.journalism.objects.minor.Student;

public class MajorObjectsHelper {

    public static void addActualPeriodToPeriodsInfo(PeriodsInfo periodsInfo, ActualPeriod period) {
        periodsInfo.addPeriod(period);
    }

    public static void addAmbigiousPeriodToPeriodsInfo(PeriodsInfo periodsInfo, AmbigiousPeriod period) {
        periodsInfo.addAmbigiousPeriod(period);
    }

    public static PersonaInfo setPersonaInfoRole(PersonaInfo personaInfo, Role role) {
        return personaInfo.setRole(role);
    }

    public static PersonaInfo setPersonaInfoGender(PersonaInfo personaInfo, Gender gender) {
        return personaInfo.setGender(gender);
    }

    public static PersonaInfo setPersonaInfoName(PersonaInfo personaInfo, String firstName, String middleName, String lastName) {
        return personaInfo.setName(firstName, middleName, lastName);
    }

    public static PersonaInfo setPersonaInfoId(PersonaInfo personaInfo, String id) {
        return personaInfo.setId(id);
    }

    public static PersonaInfo setPersonaInfoEmail(PersonaInfo personaInfo, String email) {
        return personaInfo.setEmail(email);
    }

    public static PersonaInfo setPersonaInfoMessageSignature(PersonaInfo personaInfo, String signature) {
        return personaInfo.setMessageSignature(signature);
    }

    public static PersonaInfo addStudentToPersonaInfo(PersonaInfo personaInfo, Student student) {
        return personaInfo.addStudent(student);
    }

    public static PersonaInfo setPersonaInfoCity(PersonaInfo personaInfo, String city) {
        return personaInfo.setCity(city);
    }

    public static PersonaInfo setPersonaInfoRegion(PersonaInfo personaInfo, String region) {
        return personaInfo.setRegion(region);
    }

}
