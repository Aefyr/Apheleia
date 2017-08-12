package com.aefyr.apheleia;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.Token;

/**
 * Created by Aefyr on 11.08.2017.
 */

public class Helper {
    private static Helper instance;
    private SharedPreferences preferences;

    public static Helper getInstance(Context c) {
        return instance==null?new Helper(c):instance;
    }

    private Helper(Context c) {
        preferences = PreferenceManager.getDefaultSharedPreferences(c);
        instance = this;
    }

    //Logging in...
    boolean isLoggedIn(){
        return !preferences.getString("token", "nope").equals("nope");
    }

    void saveDomain(String domain){
        preferences.edit().putString("domain", domain).apply();
    }

    String getDomain(){
        return preferences.getString("domain", "nande");
    }

    //Token
    void saveToken(Token token){
        preferences.edit().putString("token", token.getToken()).putLong("token_expires", token.getExpirationTime()).apply();
    }

    boolean isTokenExpired(){
        return System.currentTimeMillis() > preferences.getLong("token_expires", 0);
    }

    String getToken(){
        return preferences.getString("token", "nani");
    }

    //Persona
    private EljurPersona persona;
    public EljurPersona getPersona(){
        if(persona == null)
            persona = new EljurPersona(getToken(), getDomain());
        return persona;
    }

    public String getCurrentStudentId(){
        return preferences.getString("current_student", "448");
    }
}
