package com.aefyr.journalism;

import com.aefyr.apheleia.custom.ApheleiaRequest;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

class EljurApiRequests {


    //Get token to save it for further use
    static Request loginRequest(RequestQueue queue, String schoolDomain, String username, String password, final EljurApiClient.LoginRequestListener listener) {

        Request loginRequest = new ApheleiaRequest(Request.Method.GET, EljurApiRequest.HTTPS + schoolDomain + EljurApiRequest.ELJUR + "auth?" + "vendor=" + schoolDomain + "&login=" + username + "&password=" + password + EljurApiRequest.BOUND + "&_="+System.currentTimeMillis(), new Response.Listener<String>() {
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
                else if(error.networkResponse.statusCode == 403)
                    listener.onApiAccessForbidden();
                else
                    listener.onNetworkError();
            }
        });

        queue.add(loginRequest);
        return loginRequest;
    }

    //Get rules!
    static Request getRules(RequestQueue queue, EljurPersona persona, final EljurApiClient.JournalismListener<PersonaInfo> listener) {

        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_RULES);

        Request rulesRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
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
    static Request getPeriods(RequestQueue queue, EljurPersona persona, String studentId, final EljurApiClient.JournalismListener<PeriodsInfo> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_PERIODS).addParameter("student", studentId).addParameter("weeks", "__yes").addParameter("show_disabled", "__yes");

        Request periodsRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null || response.size() == 0 || response.get("students") == null || response.getAsJsonArray("students").get(0).getAsJsonObject().get("periods") == null) {
                    listener.onSuccess(null);
                    return;
                }

                JsonArray periods = response.getAsJsonArray("students").get(0).getAsJsonObject().getAsJsonArray("periods");
                PeriodsInfo periodsInfo = MajorObjectsFactory.createPeriodsInfo();

                //I'm not sure you can even get periods with no weeks inside, so this might be redundant (And I think it is), but this is Eljur we're talking about after all.
                int weeksCount = 0;

                for (JsonElement periodEl : periods) {
                    JsonObject period = periodEl.getAsJsonObject();
                    if(period.get("disabled") !=null && !period.get("disabled").isJsonNull() && period.get("disabled").getAsBoolean())
                        continue;

                    if (period.get("ambigious") != null && !period.get("ambigious").isJsonNull() && period.get("ambigious").getAsBoolean()) {
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
                            weeksCount++;
                        }

                        MajorObjectsHelper.addActualPeriodToPeriodsInfo(periodsInfo, actualPeriod);
                    }
                }

                //So we'll return null, if we have no weeks, since Apheleia can't work just with periods.
                listener.onSuccess(weeksCount==0?null:periodsInfo);
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
    static Request getDiary(RequestQueue queue, EljurPersona persona, final String studentId, String days, final boolean getTimes, final EljurApiClient.JournalismListener<DiaryEntry> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_DIARY).addParameter("days", days).addParameter("student", studentId);
        if (getTimes)
            apiRequest.addParameter("rings", "__yes");

        Request diaryRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
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
    static Request getMarks(RequestQueue queue, EljurPersona persona, final String studentId, String days, final EljurApiClient.JournalismListener<MarksGrid> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MARKS).addParameter("days", days).addParameter("student", studentId);

        Request marksRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
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
    static Request getSchedule(RequestQueue queue, EljurPersona persona, final String studentId, String days, boolean getTimes, final EljurApiClient.JournalismListener<Schedule> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_SCHEDULE).addParameter("days", days).addParameter("student", studentId);
        if (getTimes)
            apiRequest.addParameter("rings", "__yes");

        Request scheduleRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null || response.size() == 0 || response.get("students") == null) {
                    listener.onSuccess(MajorObjectsFactory.createSchedule(new ArrayList<WeekDay>(0)));
                    return;
                }

                JsonObject weekDaysObj = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonObject("days");

                ArrayList<WeekDay> weekDays = new ArrayList<>(weekDaysObj.size());
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

                    ArrayList<Lesson> lessons = null;
                    LESSONS:
                    {
                        if(weekDay.get("items")==null||(weekDay.get("items").isJsonArray() && weekDay.get("items").getAsJsonArray().size()==0))
                            break LESSONS;

                        JsonArray jLessons;
                        if(weekDay.get("items").isJsonArray()){
                            jLessons = weekDay.get("items").getAsJsonArray();
                        }else {
                            jLessons = new JsonArray();
                            JsonObject jLessonsObj = weekDay.get("items").getAsJsonObject();

                            for (Map.Entry<String, JsonElement> jLessonKey : jLessonsObj.entrySet()) {
                                jLessons.add(jLessonKey.getValue());
                            }
                        }


                        lessons = new ArrayList<>(jLessons.size());

                        for (JsonElement jLessonEl: jLessons) {
                            JsonObject lessonObj = jLessonEl.getAsJsonObject();

                            Lesson lesson = MinorObjectsFactory.createLesson(Utility.getStringFromJsonSafe(lessonObj, "num", "0"), Utility.getStringFromJsonSafe(lessonObj, "name", "Неизвестно"), Utility.getStringFromJsonSafe(lessonObj, "room", "Неизвестно"), Utility.getStringFromJsonSafe(lessonObj, "teacher", "Неизвестно"));

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
                    }

                    ArrayList<Lesson> overtimeLessons = null;
                    if (weekDay.get("items_extday") != null) {
                        JsonArray jOvertimeLessons = weekDay.getAsJsonArray("items_extday");
                        overtimeLessons = new ArrayList<>(jOvertimeLessons.size());
                        for (JsonElement otLessonEl : jOvertimeLessons) {
                            JsonObject otLessonObj = otLessonEl.getAsJsonObject();
                            Lesson otLesson = MinorObjectsFactory.createLesson("OT", Utility.getStringFromJsonSafe(otLessonObj, "name", "Неизвестно"), "OT", Utility.getStringFromJsonSafe(otLessonObj, "teacher", "Неизвестно"));

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
                        day = MinorObjectsFactory.createWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString(), lessons==null?new ArrayList<Lesson>(0):lessons);
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
    static Request getMessages(RequestQueue queue, EljurPersona persona, final MessagesList.Folder folder, boolean unreadOnly, final EljurApiClient.JournalismListener<MessagesList> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGES).addParameter("folder", folder == MessagesList.Folder.INBOX ? "inbox" : "sent").addParameter("unreadonly", String.valueOf(unreadOnly));

        Request messagesListRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
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
    static Request getMessageInfo(RequestQueue queue, EljurPersona persona, final MessagesList.Folder folder, String messageId, final int numberOfReceiversToParse, final EljurApiClient.JournalismListener<MessageInfo> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGE_INFO).addParameter("id", messageId);

        Request messageInfoRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                MessageInfoAsyncParser.getInstance().parseMessage(rawResponse, folder, numberOfReceiversToParse, listener);
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

    static Request getMessageReceivers(RequestQueue queue, EljurPersona persona, final EljurApiClient.JournalismListener<MessageReceiversInfo> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGE_RECEIVERS);

        Request messageReceiversInfoRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null) {
                    listener.onApiError(new JournalismException("no receivers"));
                    return;
                }

                JsonArray jReceiversGroups = response.getAsJsonArray("groups");
                ArrayList<MessageReceiversGroup> groups = new ArrayList<MessageReceiversGroup>(jReceiversGroups.size());

                for (JsonElement groupEl : jReceiversGroups) {
                    JsonObject group = groupEl.getAsJsonObject();

                    ArrayList<MessageReceiver> receivers;

                    if (group.get("subgroups") != null) {
                        receivers = new ArrayList<MessageReceiver>(0);
                        for (JsonElement subgroupEl : group.get("subgroups").getAsJsonArray()) {
                            JsonObject subgroup = subgroupEl.getAsJsonObject();
                            JsonArray jSubgroupMembers = subgroup.getAsJsonArray("users");
                            receivers.ensureCapacity(receivers.size()+jSubgroupMembers.size());

                            for (JsonElement receiverEl : jSubgroupMembers) {
                                JsonObject receiver = receiverEl.getAsJsonObject();

                                MessageReceiver receiverToAdd = MinorObjectsFactory.createMessageReceiver(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString(), (receiver.get("info") != null ? receiver.get("info").getAsString() : null));
                                if (!receivers.contains(receiverToAdd))
                                    receivers.add(receiverToAdd);
                            }
                        }
                    } else {
                        JsonArray jGroupMembers = group.getAsJsonArray("users");
                        receivers = new ArrayList<MessageReceiver>(jGroupMembers.size());
                        for (JsonElement receiverEl : jGroupMembers) {
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

    static Request sendMessage(RequestQueue queue, EljurPersona persona, String subject, String text, HashSet<String> receiversIds, final EljurApiClient.JournalismListener<SentMessageResponse> listener) {
        final StringBuilder r = new StringBuilder();
        for (String receiverId : receiversIds) {
            r.append(receiverId).append(",");
        }

        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.SEND_MESSAGE).addParameter("users_to", r.substring(0, r.length() - 1)).addParameter("subject", subject).addParameter("text", text);

        Request sendMessageRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getRawJsonFromResponse(rawResponse).getAsJsonObject("response");
                if(response == null){
                    MajorObjectsFactory.createSentMessageResponse(false);
                    return;
                }

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

    static Request getFinals(RequestQueue queue, EljurPersona persona, final String studentId, final EljurApiClient.JournalismListener<Finals> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_FINALS).addParameter("student", studentId);

        Request finalsRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                if (response == null || response.size() == 0 || response.get("students") == null || response.getAsJsonObject("students").getAsJsonObject(studentId).get("items") == null) {
                    listener.onSuccess(MajorObjectsFactory.createFinals(new ArrayList<FinalSubject>(0)));
                    return;
                }

                JsonArray jSubjects = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonArray("items");
                ArrayList<FinalSubject> subjects = new ArrayList<FinalSubject>(jSubjects.size());

                for (JsonElement subjectEl : jSubjects) {
                    JsonObject subject = subjectEl.getAsJsonObject();

                    if (subject.get("assessments") != null && subject.getAsJsonArray("assessments").size() > 0) {

                        JsonArray jPeriods = subject.getAsJsonArray("assessments");
                        ArrayList<FinalPeriod> periods = new ArrayList<FinalPeriod>(jPeriods.size());

                        for (JsonElement periodEl : jPeriods) {
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

    static Request hijackFCM(RequestQueue queue, EljurPersona persona, String fcmToken, final EljurApiClient.JournalismListener<Boolean> listener) {
        EljurApiRequest apiRequest = new EljurApiRequest(persona, "setpushtoken").addParameter("token", fcmToken).addParameter("type", "google").addParameter("activate", "1");

        Request tokenRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);
                if(response == null){
                    listener.onSuccess(false);
                    return;
                }

                listener.onSuccess(response.get("result").getAsBoolean());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(tokenRequest);
        return tokenRequest;
    }

    static Request getAds(RequestQueue queue, EljurPersona persona, PersonaInfo.Role role, String city, String region, String parallel, PersonaInfo.Gender gender, final EljurApiClient.JournalismListener<Boolean> listener){
        EljurApiRequest apiRequest = new EljurApiRequest(persona, "getadvertising").addParameter("platform", "android").addParameter("role", role== PersonaInfo.Role.STUDENT?"student":"parent").addParameter("region", city).addParameter("city", region).addParameter("parallel", parallel).addParameter("gender", gender== PersonaInfo.Gender.MALE?"male":"female");

        Request adsRequest = new ApheleiaRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onSuccess(true);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError(error.networkResponse != null && error.networkResponse.statusCode == 403);
            }
        });

        queue.add(adsRequest);
        return adsRequest;
    }

    //Not enough data about returned json structure to parse it
    /*static StringRequest getFeed(RequestQueue queue, EljurPersona persona, String studentId, String page){
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_FEED).addParameter("student", studentId).addParameter("page", page);
    }*/
}
