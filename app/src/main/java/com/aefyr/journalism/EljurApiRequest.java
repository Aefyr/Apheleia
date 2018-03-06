package com.aefyr.journalism;

import android.util.Pair;

import com.crashlytics.android.Crashlytics;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

class EljurApiRequest {

    //Methods
    final static String GET_RULES = "getrules";
    final static String GET_PERIODS = "getperiods";
    final static String GET_DIARY = "getdiary";
    final static String GET_MARKS = "getmarks";
    final static String GET_SCHEDULE = "getschedule";
    final static String GET_MESSAGES = "getmessages";
    final static String GET_MESSAGE_INFO = "getmessageinfo";
    final static String GET_MESSAGE_RECEIVERS = "getmessagereceivers";
    final static String SEND_MESSAGE = "sendmessage";
    final static String GET_FINALS = "getfinalassessments";
    public final static String GET_FEED = "getupdates";

    static final String HTTPS = "https://";
    static final String ELJUR = ".eljur.ru/apiv3/";
    static final String BOUND = "&devkey=6ec0e964a29c22fe5542f748b5143c4e&out_format=json";

    private EljurPersona persona;
    private String method;
    private ArrayList<Pair<String, String>> parameters;

    EljurApiRequest(EljurPersona persona, String method) {
        this.persona = persona;
        this.method = method;
        parameters = new ArrayList<>(4);
    }

    EljurApiRequest addParameter(String name, String value) {
        parameters.add(new Pair<String, String>(name, value));
        return this;
    }

    /*String getRequestURL() {
        StringBuilder httpRequest = new StringBuilder(HTTPS + persona.schoolDomain + ELJUR + method + "?" + "vendor=" + persona.schoolDomain + BOUND + "&auth_token=" + persona.token + "&_="+System.currentTimeMillis());

        for (String k : parameters.keySet())
            try {
                httpRequest.append("&").append(k).append("=").append(URLEncoder.encode(parameters.get(k), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                //Well, it shouldn't happen I guess...
                //TODO Gotta ad Firebase report here
            }
        return httpRequest.toString();
    }*/

    String getRequestURL(){
        StringBuilder httpRequest = new StringBuilder(HTTPS).append(persona.schoolDomain).append(ELJUR).append(method).append("?");

        for(int i = 0; i<parameters.size(); i++){
            if(i!=0)
                httpRequest.append("&");

            Pair<String, String> param = parameters.get(i);
            try {
                httpRequest.append(param.first).append("=").append(URLEncoder.encode(param.second, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
        }

        if(parameters.size()!=0)
            httpRequest.append("&");

        httpRequest.append("auth_token=").append(persona.token).append("&vendor=").append(persona.schoolDomain).append(BOUND).append("&_=").append(System.currentTimeMillis());

        return httpRequest.toString();
    }
}
