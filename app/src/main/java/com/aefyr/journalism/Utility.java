package com.aefyr.journalism;

import android.util.Log;

import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class Utility {


    public static JsonObject getJsonFromResponse(String rawResponse) {
        try {
            return new JsonParser().parse(rawResponse).getAsJsonObject().getAsJsonObject("response").getAsJsonObject("result");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Crashlytics.log(e.getMessage());
                Crashlytics.logException(new JournalismException("Unable to get response Json object"));
            }catch (IllegalStateException e1){
                Log.wtf("Journalism", "Unable to report an exception that has occurred during Json parsing to Firebase");
            }

            return null;
        }

    }

    public static JsonObject getRawJsonFromResponse(String rawResponse) {
        try {
            return new JsonParser().parse(rawResponse).getAsJsonObject();
        } catch (Exception e){
            e.printStackTrace();
            try {
                Crashlytics.log(e.getMessage());
                Crashlytics.logException(new JournalismException("Unable to get response Json object"));
            }catch (IllegalStateException e1){
                Log.wtf("Journalism", "Unable to report an exception that has occurred during Json parsing to Firebase");
            }
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

    public static String getStringFromJsonSafe(JsonObject jsonObject, String key, String defaultValue){
        try {
            return jsonObject.get(key).getAsString();
        }catch (Exception e){
            e.printStackTrace();
            Crashlytics.log(e.getMessage());
            Crashlytics.logException(new JournalismException("Unable to retrieve key \""+key+"\" from JsonObject: "+jsonObject.toString()));
            return defaultValue;
        }
    }

}
