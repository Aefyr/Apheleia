package com.aefyr.apheleia.custom;

import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by Aefyr on 06.12.2017.
 */

public class ApheleiaRequest extends Request<String> {
    private static final String TAG = "ApheleiaRequest";

    private Response.Listener<String> listener;
    private static String userAgent = String.format("Mozilla/5.0 (Linux; Android %s; %s Build/%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Crosswalk/23.53.589.4 Mobile Safari/537.36", Build.VERSION.RELEASE, Build.MODEL, Build.ID);

    private static String ejId;
    private static long ejIdExpirationTime = 0;

    public ApheleiaRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
        setRetryPolicy(new DefaultRetryPolicy(5000, 2, 1.5f));
        Log.d(TAG, "Created request with URL: " + url);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {

            //Eljur API says content is in gzip encoding in some requests when it's not
            String responseS;
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(response.data)); BufferedReader in = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))){
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null)
                    sb.append(line);

                responseS = sb.toString();
            }catch (Exception e){
                responseS = new String(response.data, StandardCharsets.UTF_8);
            }

            PARSING_COOKIES:
            {
                if (System.currentTimeMillis() > ejIdExpirationTime) {
                    String cookie = response.headers.get("Set-Cookie");
                    if (cookie == null)
                        break PARSING_COOKIES;

                    String[] cookieMeta = cookie.split(";");
                    if (cookieMeta.length < 3)
                        break PARSING_COOKIES;

                    String[] cookieData = cookieMeta[0].split("=");
                    if (cookieData.length < 2)
                        break PARSING_COOKIES;

                    ejId = cookieData[1];
                    try {
                        ejIdExpirationTime = System.currentTimeMillis() + Long.parseLong(cookieMeta[2].split("=")[1]) * 1000L;
                        Log.d(TAG, "Got ejId cookie " + ejId + ", expires at " + ejIdExpirationTime);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to parse ejId cookie expiration time:");
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                }
            }

            Log.d(TAG, "Got response for url " + getUrl() + ":\n" + responseS);
            return Response.success(responseS, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(String response) {
        listener.onResponse(response);
    }


    @Override
    public void deliverError(VolleyError error) {
        super.deliverError(error);
        Log.e(TAG, "Request with url " + getUrl() + " returned an error:\n" + error.getCause());
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Connection", "keep-alive");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", userAgent);
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "ru-ru");

        if (ejId != null)
            headers.put("Cookie", "ej_id=" + ejId);

        return headers;
    }
}
