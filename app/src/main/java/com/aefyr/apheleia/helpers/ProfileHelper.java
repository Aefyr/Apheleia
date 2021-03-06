package com.aefyr.apheleia.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.minor.Student;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Aefyr on 13.08.2017.
 */

public class ProfileHelper {
    private static ProfileHelper instance;
    private SharedPreferences preferences;
    private boolean demoMode;

    private ProfileHelper(Context c) {
        instance = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(c);
        demoMode = preferences.getBoolean("debug_fake_name", false);
    }

    public static ProfileHelper getInstance(Context c) {
        return instance == null ? new ProfileHelper(c) : instance;
    }

    public void saveStudentsIds(Set<String> studentIds) {
        preferences.edit().putStringSet("students", studentIds).apply();
    }

    public Set<String> getStudentsIds() {
        return preferences.getStringSet("students", null);
    }

    public String getCurrentStudentId() {
        return preferences.getString("current_student", "000");
    }

    public void setCurrentStudent(String studentId) {
        preferences.edit().putString("current_student", studentId).apply();
    }

    public String getStudentName(String studentId) {
        return demoMode ? "Немцов Руслан" : preferences.getString("student_name_" + studentId, "Tony Barrera");
    }

    public void setStudentName(String name, String studentId) {
        preferences.edit().putString("student_name_" + studentId, name).apply();
    }

    public String getStudentClass(String studentId) {
        return demoMode ? "5В" : preferences.getString("student_class_" + studentId, "999X");
    }

    public void setStudentClass(String clazz, String studentId) {
        preferences.edit().putString("student_class_" + studentId, clazz).apply();
    }

    public String getGender() {
        return preferences.getString("gender", "f");
    }

    private void setGender(String gender) {
        preferences.edit().putString("gender", gender).apply();
    }

    public String getEmail() {
        return preferences.getString("email", "n");
    }

    private void setEmail(String email) {
        preferences.edit().putString("email", email).apply();
    }

    public String getName() {
        return demoMode ? "Агния Немцова" : preferences.getString("name", "n");
    }

    public void setName(String name) {
        preferences.edit().putString("name", name).apply();
    }

    public int getStudentsCount() {
        return preferences.getInt("students_count", 1);
    }

    public class Role {
        public final static String STUDENT = "student";
        public final static String PARENT = "parent";
    }

    private void setRole(String role) {
        preferences.edit().putString("role", role).apply();
    }

    public String getRole() {
        return preferences.getString("role", "na");
    }

    private void setStudentsCount(int studentsCount) {
        preferences.edit().putInt("students_count", studentsCount).apply();
    }

    private void setCity(String city) {
        preferences.edit().putString("city", city).apply();
    }

    private void setRegion(String region) {
        preferences.edit().putString("region", region).apply();
    }

    private void setSign(String sign) {
        preferences.edit().putString("sign", sign).apply();
    }


    public void savePersonaInfo(PersonaInfo personaInfo) {
        setGender(personaInfo.gender() == PersonaInfo.Gender.FEMALE ? "f" : "m");
        setEmail(personaInfo.email());
        setName(personaInfo.getCompositeName(true, false, true));
        setRole(personaInfo.role() == PersonaInfo.Role.PARENT ? Role.PARENT : Role.STUDENT);

        setSign(personaInfo.sign());
        setCity(personaInfo.city());
        setRegion(personaInfo.region());


        HashSet<String> studentIds = new HashSet<>();
        String lastId = null;
        for (Student student : personaInfo.getStudents()) {
            studentIds.add(student.id());
            lastId = student.id();
            setStudentName(student.name(), lastId);
            setStudentClass(student.className(), lastId);
        }

        saveStudentsIds(studentIds);
        setCurrentStudent(lastId);
        setStudentsCount(studentIds.size());
    }

    static void destroy() {
        instance = null;
    }
}
