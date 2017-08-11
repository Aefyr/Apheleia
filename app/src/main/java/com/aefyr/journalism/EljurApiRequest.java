package com.aefyr.journalism;

import java.util.HashMap;

class EljurApiRequest {
	
	//Methods
	public final static String GET_RULES = "getrules";
	public final static String GET_PERIODS = "getperiods";
	public final static String GET_DIARY = "getdiary";
	public final static String GET_MARKS = "getmarks";
	public final static String GET_SCHEDULE = "getschedule";
	public final static String GET_MESSAGES = "getmessages";
	public final static String GET_MESSAGE_INFO = "getmessageinfo";
	public final static String GET_MESSAGE_RECEIVERS = "getmessagereceivers";
	public final static String SEND_MESSAGE = "sendmessage";
	public final static String GET_FINALS = "getfinalassessments";
	public final static String GET_FEED = "getupdates";
	
	static final String HTTPS = "https://";
	static final String ELJUR = ".eljur.ru/apiv3/";
	static final String BOUND = "devkey=6ec0e964a29c22fe5542f748b5143c4e&out_format=json";
	
	private EljurPersona persona;
	private String method;
	private HashMap<String, String> parameters;
	
	EljurApiRequest(EljurPersona persona, String method){
		this.persona = persona;
		this.method = method;
		parameters = new HashMap<String, String>();
	}
	
	EljurApiRequest addParameter(String name, String value){
		parameters.put(name, value);
		return this;
	}
	
	String getRequestURL(){
		String httpRequest = HTTPS+persona.schoolDomain+ELJUR+method+"?"+BOUND+"&vendor="+persona.schoolDomain+"&auth_token="+persona.token;
		for(String k:parameters.keySet())
			httpRequest+="&"+k+"="+parameters.get(k);
		return httpRequest;
	}
}
