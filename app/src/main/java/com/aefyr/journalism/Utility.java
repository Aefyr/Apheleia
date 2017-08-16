package com.aefyr.journalism;

import com.aefyr.journalism.objects.major.PersonaInfo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class Utility {
	
	
	public static JsonObject getJsonFromResponse(String rawResponse){
		return new JsonParser().parse(rawResponse).getAsJsonObject().getAsJsonObject("response").getAsJsonObject("result");
	}

	public static JsonObject getRawJsonFromResponse(String rawResponse){
		return new JsonParser().parse(rawResponse).getAsJsonObject();
	}
	
	public static PersonaInfo.Gender parseGender(String genderString){
		if(genderString.equals("female"))
			return PersonaInfo.Gender.FEMALE;
		else if (genderString.equals("male"))
			return PersonaInfo.Gender.MALE;
		return PersonaInfo.Gender.UNKNOWN;
	}

}
