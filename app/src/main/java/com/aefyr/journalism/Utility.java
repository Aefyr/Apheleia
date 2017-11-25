package com.aefyr.journalism;

import com.aefyr.journalism.objects.major.PersonaInfo;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class Utility {


    public static JsonObject getJsonFromResponse(String rawResponse) {
        try {
            return new JsonParser().parse(rawResponse).getAsJsonObject().getAsJsonObject("response").getAsJsonObject("result");
        } catch (Exception e) {
            FirebaseCrash.report(e);
            return null;
        }

    }

    public static JsonObject getRawJsonFromResponse(String rawResponse) {
        try {
            return new JsonParser().parse(rawResponse).getAsJsonObject();
        } catch (Exception e){
            FirebaseCrash.report(e);
            return null;
        }
    }

    public static PersonaInfo.Gender parseGender(String genderString) {
        if (genderString.equals("female"))
            return PersonaInfo.Gender.FEMALE;
        else if (genderString.equals("male"))
            return PersonaInfo.Gender.MALE;
        return PersonaInfo.Gender.UNKNOWN;
    }

}
