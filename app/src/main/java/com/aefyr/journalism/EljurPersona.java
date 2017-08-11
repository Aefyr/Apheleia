package com.aefyr.journalism;


import com.aefyr.journalism.objects.major.Token;

public class EljurPersona {
	
	protected String schoolDomain;
	protected String token;
	
	public EljurPersona(String token, String schoolDomain){
		this.token = token;
		this.schoolDomain = schoolDomain;
	}
	
	public EljurPersona(Token token, String schoolDomain){
		this.token = token.getToken();
		this.schoolDomain = schoolDomain;
	}
	
}
