package com.aefyr.journalism;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

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
    static final String BOUND = "devkey=6ec0e964a29c22fe5542f748b5143c4e&out_format=json";

    private EljurPersona persona;
    private String method;
    private HashMap<String, String> parameters;

    EljurApiRequest(EljurPersona persona, String method) {
        this.persona = persona;
        this.method = method;
        parameters = new HashMap<String, String>();
    }

    EljurApiRequest addParameter(String name, String value) {
        parameters.put(name, value);
        return this;
    }

    String getRequestURL() {
        StringBuilder httpRequest = new StringBuilder(HTTPS + persona.schoolDomain + ELJUR + method + "?" + BOUND + "&vendor=" + persona.schoolDomain + "&auth_token=" + persona.token);

        for (String k : parameters.keySet())
            try {
                httpRequest.append("&").append(k).append("=").append(URLEncoder.encode(parameters.get(k), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                //Well, it shouldn't happen I guess...
                //TODO Gotta ad Firebase report here
            }
        return httpRequest.toString();
    }
}
