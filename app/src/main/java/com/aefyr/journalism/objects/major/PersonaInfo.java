package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.Student;

import java.util.ArrayList;


public class PersonaInfo {
	public enum Role{
		STUDENT, PARENT
	};
	
	public enum Gender{
		MALE, FEMALE, UNKNOWN;
	}
	
	Role role;
	Gender gender = Gender.UNKNOWN;
	
	String firstName;
	String middleName;
	String lastName;

	String id;
	String email;
	String messageSignature;
	
	ArrayList<Student> students;
	
	public PersonaInfo(){
		students = new ArrayList<Student>();
	}
	
	PersonaInfo setRole(Role role){
		this.role = role;
		return this;
	}
	
	PersonaInfo setGender(Gender gender){
		this.gender = gender;
		return this;
	}
	
	PersonaInfo setName(String firstName, String middleName, String lastName){
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		return this;
	}
	
	PersonaInfo setId(String id){
		this.id = id;
		return this;
	}
	
	PersonaInfo setEmail(String email){
		this.email = email;
		return this;
	}
	
	PersonaInfo setMessageSignature(String signature){
		messageSignature = signature;
		return this;
	}
	
	PersonaInfo addStudent(Student student){
		students.add(student);
		return this;
	}
	
	public Role role(){
		return role;
	}
	
	public Gender gender(){
		return gender;
	}
	
	public String getCompositeName(boolean f, boolean m, boolean l){
		String compositeName = "";
		if(f)
			compositeName+=firstName;
		if(m)
			compositeName+=" "+middleName;
		if(l)
			compositeName+=" "+lastName;
		return compositeName;
	}
	
	public String id(){
		return id;
	}
	
	public String email(){
		return email;
	}
	
	public String messageSignature(){
		return messageSignature;
	}
	
	public ArrayList<Student> getStudents(){
		return students;
	}
	
}
