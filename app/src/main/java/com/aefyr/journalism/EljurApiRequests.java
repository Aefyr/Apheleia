package com.aefyr.journalism;

import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.major.Finals;
import com.aefyr.journalism.objects.major.MajorObjectsFactory;
import com.aefyr.journalism.objects.major.MajorObjectsHelper;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.aefyr.journalism.objects.major.MessageReceiversInfo;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.major.PeriodsInfo;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.major.Schedule;
import com.aefyr.journalism.objects.major.SentMessageResponse;
import com.aefyr.journalism.objects.major.Token;
import com.aefyr.journalism.objects.minor.ActualPeriod;
import com.aefyr.journalism.objects.minor.FinalPeriod;
import com.aefyr.journalism.objects.minor.FinalSubject;
import com.aefyr.journalism.objects.minor.Lesson;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.aefyr.journalism.objects.minor.MessageReceiver;
import com.aefyr.journalism.objects.minor.MessageReceiversGroup;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.MinorObjectsHelper;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.aefyr.journalism.objects.utility.WeekDaysComparator;
import com.aefyr.journalism.parsing.DiaryAsyncParser;
import com.aefyr.journalism.parsing.MarkGridAsyncParser;
import com.aefyr.journalism.parsing.MessageInfoAsyncParser;
import com.aefyr.journalism.parsing.MessagesListAsyncParser;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

class EljurApiRequests {


    //Get token to save it for further use
    static StringRequest loginRequest(RequestQueue queue, String schoolDomain, String username, String password, final EljurApiClient.LoginRequestListener listener) {

        StringRequest loginRequest = new StringRequest(Request.Method.GET, EljurApiRequest.HTTPS + schoolDomain + EljurApiRequest.ELJUR + "auth?" + EljurApiRequest.BOUND + "&vendor=" + schoolDomain + "&login=" + username + "&password=" + password, new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject result = Utility.getJsonFromResponse(rawResponse);

                if (result == null) {
                    listener.onApiError(new JournalismException("no token returned"));
                    return;
                }

                try {
                    listener.onSuccessfulLogin(Token.createToken(result.get("token").getAsString(), result.get("expires").getAsString()));
                } catch (JournalismException e) {
                    listener.onApiError(e);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse == null)
                    listener.onNetworkError();
                else if (error.networkResponse.statusCode == 400)
                    listener.onInvalidCredentialsError();
                else if (error.networkResponse.statusCode == 500)
                    listener.onInvalidDomainError();
            }
        });

        queue.add(loginRequest);
        return loginRequest;
    }

    //Get rules!
    static StringRequest getRules(RequestQueue queue, EljurPersona persona, final EljurApiClient.JournalismListener<PersonaInfo> listener) {

        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_RULES).addParameter("vendor", persona.schoolDomain);

        StringRequest rulesRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null) {
                    listener.onApiError(new JournalismException("no rules"));
                    return;
                }

                PersonaInfo personaInfo = new PersonaInfo();

                String roles = response.getAsJsonArray("roles").toString();
                if (roles.contains("parent"))
                    MajorObjectsHelper.setPersonaInfoRole(personaInfo, PersonaInfo.Role.PARENT);
                else if (roles.contains("student"))
                    MajorObjectsHelper.setPersonaInfoRole(personaInfo, PersonaInfo.Role.STUDENT);
                else {
                    listener.onApiError(new JournalismException("unsupported role"));
                    return;
                }

                MajorObjectsHelper.setPersonaInfoGender(personaInfo, Utility.parseGender(response.get("gender").getAsString()));
                MajorObjectsHelper.setPersonaInfoId(personaInfo, response.get("name").getAsString());
                MajorObjectsHelper.setPersonaInfoEmail(personaInfo, response.get("email").getAsString());
                MajorObjectsHelper.setPersonaInfoName(personaInfo, response.get("firstname").getAsString(), response.get("middlename").getAsString(), response.get("lastname").getAsString());
                MajorObjectsHelper.setPersonaInfoMessageSignature(personaInfo, response.get("messageSignature").getAsString());
                MajorObjectsHelper.setPersonaInfoCity(personaInfo, response.get("city").getAsString());
                MajorObjectsHelper.setPersonaInfoRegion(personaInfo, response.get("region").getAsString());

                if (response.get("relations") == null || response.getAsJsonObject("relations").get("students") == null) {
                    listener.onApiError(new JournalismException("no relations"));
                    return;
                }

                JsonObject students = response.getAsJsonObject("relations").getAsJsonObject("students");


                for (Map.Entry<String, JsonElement> entry : students.entrySet()) {
                    JsonObject student = entry.getValue().getAsJsonObject();
                    MajorObjectsHelper.addStudentToPersonaInfo(personaInfo, MinorObjectsFactory.createStudent(student.get("title").getAsString(), student.get("name").getAsString(), Utility.parseGender(student.get("gender").getAsString()), student.get("class").getAsString()));
                }

                listener.onSuccess(personaInfo);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(rulesRequest);
        return rulesRequest;
    }

    //Get periods!
    static StringRequest getPeriods(RequestQueue queue, EljurPersona persona, String studentId, final EljurApiClient.JournalismListener<PeriodsInfo> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_PERIODS).addParameter("weeks", "__yes").addParameter("student", studentId);

        StringRequest periodsRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null) {
                    listener.onApiError(new JournalismException("no periods"));
                    return;
                }

                JsonArray periods = response.getAsJsonArray("students").get(0).getAsJsonObject().getAsJsonArray("periods");
                PeriodsInfo periodsInfo = MajorObjectsFactory.createPeriodsInfo();

                for (JsonElement periodEl : periods) {
                    JsonObject period = periodEl.getAsJsonObject();
                    if (period.get("ambigious").getAsBoolean()) {
                        MajorObjectsHelper.addAmbigiousPeriodToPeriodsInfo(periodsInfo, MinorObjectsFactory.createAmbigiousPeriod(period.get("name").getAsString(), period.get("fullname").getAsString()));
                    } else {
                        ActualPeriod actualPeriod;

                        try {
                            actualPeriod = MinorObjectsFactory.createActualPeriod(period.get("name").getAsString(), period.get("fullname").getAsString(), period.get("start").getAsString(), period.get("end").getAsString());
                        } catch (JournalismException e) {
                            listener.onApiError(e);
                            return;
                        }

                        JsonArray weeks = period.getAsJsonArray("weeks");

                        for (JsonElement weekEl : weeks) {
                            JsonObject week = weekEl.getAsJsonObject();
                            try {
                                MinorObjectsHelper.addWeekToActualPeriod(actualPeriod, MinorObjectsFactory.createWeek(week.get("start").getAsString(), week.get("end").getAsString(), week.get("title").getAsString()));
                            } catch (JournalismException e) {
                                listener.onApiError(e);
                                return;
                            }
                        }

                        MajorObjectsHelper.addActualPeriodToPeriodsInfo(periodsInfo, actualPeriod);
                    }
                }

                listener.onSuccess(periodsInfo);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(periodsRequest);

        return periodsRequest;
    }

    //Get diary!
    static StringRequest getDiary(RequestQueue queue, EljurPersona persona, final String studentId, String days, final boolean getTimes, final EljurApiClient.JournalismListener<DiaryEntry> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_DIARY).addParameter("student", studentId).addParameter("days", days);
        if (getTimes)
            apiRequest.addParameter("rings", "__yes");

        StringRequest diaryRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                DiaryAsyncParser.getInstance().parseDiary(rawResponse, studentId, listener);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(diaryRequest);
        return diaryRequest;
    }

    //Get marks!
    static StringRequest getMarks(RequestQueue queue, EljurPersona persona, final String studentId, String days, final EljurApiClient.JournalismListener<MarksGrid> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MARKS).addParameter("student", studentId).addParameter("days", days);

        StringRequest marksRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                MarkGridAsyncParser.getInstance().parseGrid(rawResponse, studentId, listener);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(marksRequest);


        return marksRequest;
    }

    //Get schedule!
    static StringRequest getSchedule(RequestQueue queue, EljurPersona persona, final String studentId, String days, boolean getTimes, final EljurApiClient.JournalismListener<Schedule> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_SCHEDULE).addParameter("studentId", studentId).addParameter("days", days);
        if (getTimes)
            apiRequest.addParameter("rings", "__yes");

        StringRequest scheduleRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null || response.size() == 0 || response.get("students") == null) {
                    listener.onSuccess(MajorObjectsFactory.createSchedule(new ArrayList<WeekDay>(0)));
                    return;
                }

                JsonObject weekDaysObj = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonObject("days");

                ArrayList<WeekDay> weekDays = new ArrayList<>(5);
                for (Map.Entry<String, JsonElement> entry : weekDaysObj.entrySet()) {
                    JsonObject weekDay = entry.getValue().getAsJsonObject();

                    if (weekDay.get("alert") != null && weekDay.get("alert").getAsString().equals("vacation")) {
                        try {
                            weekDays.add(MinorObjectsFactory.createVacationWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString()));
                        } catch (JournalismException e) {
                            listener.onApiError(e);
                            return;
                        }
                        continue;
                    }
                    ArrayList<Lesson> lessons = new ArrayList<Lesson>();
                    for (Map.Entry<String, JsonElement> entry2 : weekDay.getAsJsonObject("items").entrySet()) {
                        JsonObject lessonObj = entry2.getValue().getAsJsonObject();

                        Lesson lesson = MinorObjectsFactory.createLesson(lessonObj.get("num").getAsString(), lessonObj.get("name").getAsString(), lessonObj.get("room").getAsString(), lessonObj.get("teacher").getAsString());

                        if (lessonObj.get("starttime") != null && lessonObj.get("endtime") != null) {
                            try {
                                MinorObjectsHelper.addTimesToLesson(lesson, lessonObj.get("starttime").getAsString(), lessonObj.get("endtime").getAsString());
                            } catch (JournalismException e) {
                                listener.onApiError(e);
                                return;
                            }
                        }

                        lessons.add(lesson);
                    }

                    ArrayList<Lesson> overtimeLessons = null;
                    if (weekDay.get("items_extday") != null) {
                        overtimeLessons = new ArrayList<>();
                        for (JsonElement otLessonEl : weekDay.getAsJsonArray("items_extday")) {
                            JsonObject otLessonObj = otLessonEl.getAsJsonObject();
                            Lesson otLesson = MinorObjectsFactory.createLesson("OT", otLessonObj.get("name").getAsString(), "OT", otLessonObj.get("teacher").getAsString());

                            if (otLessonObj.get("starttime") != null && otLessonObj.get("endtime") != null) {
                                try {
                                    MinorObjectsHelper.addTimesToLesson(otLesson, otLessonObj.get("starttime").getAsString(), otLessonObj.get("endtime").getAsString());
                                } catch (JournalismException e) {
                                    listener.onApiError(e);
                                    return;
                                }
                            }

                            overtimeLessons.add(otLesson);
                        }
                    }

                    WeekDay day;
                    try {
                        day = MinorObjectsFactory.createWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString(), lessons);
                    } catch (JournalismException e) {
                        listener.onApiError(e);
                        return;
                    }

                    if (overtimeLessons != null)
                        MinorObjectsHelper.addOvertimeLessonsToWeekDat(day, overtimeLessons);
                    weekDays.add(day);
                }
                Collections.sort(weekDays, new WeekDaysComparator());
                listener.onSuccess(MajorObjectsFactory.createSchedule(weekDays));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(scheduleRequest);
        return scheduleRequest;
    }

    //Get messages list!
    static StringRequest getMessages(RequestQueue queue, EljurPersona persona, final MessagesList.Folder folder, boolean unreadOnly, final EljurApiClient.JournalismListener<MessagesList> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGES).addParameter("folder", folder == MessagesList.Folder.INBOX ? "inbox" : "sent").addParameter("unreadonly", String.valueOf(unreadOnly));

        StringRequest messagesListRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                MessagesListAsyncParser.getInstance().parseMessages(rawResponse, folder, listener);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(messagesListRequest);
        return messagesListRequest;
    }

    //Get message info!
    static StringRequest getMessageInfo(RequestQueue queue, EljurPersona persona, final MessagesList.Folder folder, String messageId, final EljurApiClient.JournalismListener<MessageInfo> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGE_INFO).addParameter("id", messageId);

        StringRequest messageInfoRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                MessageInfoAsyncParser.getInstance().parseMessage(rawResponse, folder, listener);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(messageInfoRequest);
        return messageInfoRequest;
    }

    static StringRequest getMessageReceivers(RequestQueue queue, EljurPersona persona, final EljurApiClient.JournalismListener<MessageReceiversInfo> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGE_RECEIVERS);

        StringRequest messageReceiversInfoRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null) {
                    listener.onApiError(new JournalismException("no receivers"));
                    return;
                }

                ArrayList<MessageReceiversGroup> groups = new ArrayList<MessageReceiversGroup>();

                for (JsonElement groupEl : response.getAsJsonArray("groups")) {
                    JsonObject group = groupEl.getAsJsonObject();

                    ArrayList<MessageReceiver> receivers = new ArrayList<MessageReceiver>();

                    if (group.get("subgroups") != null) {
                        for (JsonElement subgroupEl : group.get("subgroups").getAsJsonArray()) {
                            JsonObject subgroup = subgroupEl.getAsJsonObject();
                            for (JsonElement receiverEl : subgroup.getAsJsonArray("users")) {
                                JsonObject receiver = receiverEl.getAsJsonObject();

                                MessageReceiver receiverToAdd = MinorObjectsFactory.createMessageReceiver(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString(), (receiver.get("info") != null ? receiver.get("info").getAsString() : null));
                                if (!receivers.contains(receiverToAdd))
                                    receivers.add(receiverToAdd);
                            }
                        }
                    } else {
                        for (JsonElement receiverEl : group.getAsJsonArray("users")) {
                            JsonObject receiver = receiverEl.getAsJsonObject();

                            MessageReceiver receiverToAdd = MinorObjectsFactory.createMessageReceiver(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString(), (receiver.get("info") != null ? receiver.get("info").getAsString() : null));
                            if (!receivers.contains(receiverToAdd))
                                receivers.add(receiverToAdd);
                        }
                    }

                    groups.add(MinorObjectsFactory.createMessageReceiversGroup(group.get("key").getAsString(), group.get("name").getAsString(), receivers));
                }

                listener.onSuccess(MajorObjectsFactory.createMessageReceiversInfo(groups));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(messageReceiversInfoRequest);
        return messageReceiversInfoRequest;
    }

    static StringRequest sendMessage(RequestQueue queue, EljurPersona persona, String subject, String text, HashSet<String> receiversIds, final EljurApiClient.JournalismListener<SentMessageResponse> listener) {
        final StringBuilder r = new StringBuilder();
        for (String receiverId : receiversIds) {
            r.append(receiverId).append(",");
        }

        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.SEND_MESSAGE).addParameter("users_to", r.substring(0, r.length() - 1)).addParameter("subject", subject).addParameter("text", text);

        StringRequest sendMessageRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getRawJsonFromResponse(rawResponse).getAsJsonObject("response");

                if (response.get("state").getAsInt() == 200 && response.get("error").isJsonNull())
                    listener.onSuccess(MajorObjectsFactory.createSentMessageResponse(true));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(sendMessageRequest);
        return sendMessageRequest;
    }

    static StringRequest getFinals(RequestQueue queue, EljurPersona persona, final String studentId, final EljurApiClient.JournalismListener<Finals> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_FINALS).addParameter("student", studentId);

        StringRequest finalsRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null || response.size() == 0 || response.get("students") == null || response.getAsJsonObject("students").getAsJsonObject(studentId).get("items") == null) {
                    listener.onSuccess(MajorObjectsFactory.createFinals(new ArrayList<FinalSubject>(0)));
                    return;
                }

                ArrayList<FinalSubject> subjects = new ArrayList<FinalSubject>();

                for (JsonElement subjectEl : response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonArray("items")) {
                    JsonObject subject = subjectEl.getAsJsonObject();

                    if (subject.get("assessments") != null && subject.getAsJsonArray("assessments").size() > 0) {
                        ArrayList<FinalPeriod> periods = new ArrayList<FinalPeriod>();

                        for (JsonElement periodEl : subject.getAsJsonArray("assessments")) {
                            JsonObject period = periodEl.getAsJsonObject();

                            periods.add(MinorObjectsFactory.createFinalPeriod(period.get("period").getAsString(), period.get("value").getAsString(), (period.get("comment") != null && period.get("comment").getAsString().length() > 0) ? period.get("comment").getAsString() : null));
                        }

                        subjects.add(MinorObjectsFactory.createFinalSubject(subject.get("name").getAsString(), periods));
                    } else {
                        subjects.add(MinorObjectsFactory.createFinalSubject(subject.get("name").getAsString(), null));
                    }
                }

                listener.onSuccess(MajorObjectsFactory.createFinals(subjects));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(finalsRequest);
        return finalsRequest;
    }

    //Not enough data about returned json structure to parse it
    /*static StringRequest getFeed(RequestQueue queue, EljurPersona persona, String studentId, String page){
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_FEED).addParameter("student", studentId).addParameter("page", page);
    }*/
}
