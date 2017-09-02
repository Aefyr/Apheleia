package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.exceptions.JournalismException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Token {
    private String token;
    private long expirationTime;

    public static Token createToken(String token, String rawExpiration) throws JournalismException {
        Token t = new Token();
        t.token = token;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
        try {
            t.expirationTime = format.parse(rawExpiration + " +3000").getTime();
        } catch (ParseException e) {
            throw new JournalismException("Couldn't parse token expiration date\n" + e.getMessage());
        }
        return t;
    }

    public String getToken() {
        return token;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
