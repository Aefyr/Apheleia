package com.aefyr.apheleia.helpers;

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
        return instance == null ? new Helper(c) : instance;
    }

    private Helper(Context c) {
        preferences = PreferenceManager.getDefaultSharedPreferences(c);
        instance = this;
    }

    //Logging in...
    public boolean isTokenSaved() {
        return !preferences.getString("token", "nope").equals("nope");
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean("logged_in", false);
    }

    public void setLoggedIn(boolean loggedIn) {
        preferences.edit().putBoolean("logged_in", loggedIn).apply();
    }

    public void saveDomain(String domain) {
        preferences.edit().putString("domain", domain).apply();
    }

    public String getDomain() {
        return preferences.getString("domain", "nande");
    }

    //Token
    public void saveToken(Token token) {
        preferences.edit().putString("token", token.getToken()).putLong("token_expires", token.getExpirationTime()).apply();
    }

    public boolean isTokenExpired() {
        return System.currentTimeMillis() > preferences.getLong("token_expires", 0);
    }

    String getToken() {
        return preferences.getString("token", "nani");
    }

    //Persona
    private EljurPersona persona;

    public EljurPersona getPersona() {
        if (persona == null)
            persona = new EljurPersona(getToken(), getDomain());
        return persona;
    }

    public static void destroy() {
        instance = null;
    }


}
