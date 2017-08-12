package com.aefyr.journalism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


import com.aefyr.journalism.exceptions.EljurApiException;
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
import com.aefyr.journalism.objects.minor.Attachment;
import com.aefyr.journalism.objects.minor.FinalPeriod;
import com.aefyr.journalism.objects.minor.FinalSubject;
import com.aefyr.journalism.objects.minor.GridMark;
import com.aefyr.journalism.objects.minor.Hometask;
import com.aefyr.journalism.objects.minor.Homework;
import com.aefyr.journalism.objects.minor.Lesson;
import com.aefyr.journalism.objects.minor.Mark;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.aefyr.journalism.objects.minor.MessagePerson;
import com.aefyr.journalism.objects.minor.MessageReceiver;
import com.aefyr.journalism.objects.minor.MessageReceiversGroup;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.MinorObjectsHelper;
import com.aefyr.journalism.objects.minor.ShortMessage;
import com.aefyr.journalism.objects.minor.SubjectInGrid;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.aefyr.journalism.objects.utility.HometasksComparator;
import com.aefyr.journalism.objects.utility.WeekDaysComparator;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class EljurApiRequests {
	
	
	//Get token to save it for further use
	static StringRequest loginRequest(RequestQueue queue, String schoolDomain, String username, String password, final EljurApiClient.LoginRequestListener listener){

        StringRequest loginRequest = new StringRequest(Request.Method.GET, EljurApiRequest.HTTPS+schoolDomain+EljurApiRequest.ELJUR+"auth?"+EljurApiRequest.BOUND+"&vendor="+schoolDomain+"&login="+username+"&password="+password, new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject result = Utility.getJsonFromResponse(rawResponse);

                try {
                    listener.onSuccessfulLogin(Token.createToken(result.get("token").getAsString(), result.get("expires").getAsString()));
                } catch (EljurApiException e) {
                    listener.onApiError(e.getMessage());
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse.statusCode==400)
                    listener.onInvalidCredentialsError();
                else if(error.networkResponse.statusCode==500)
                    listener.onInvalidDomainError();
                else
                    listener.onNetworkError();
            }
        });

        queue.add(loginRequest);
        return loginRequest;
	}
	
	//Get rules!
	static StringRequest getRules(RequestQueue queue, EljurPersona persona, final EljurApiClient.JournalismListener<PersonaInfo> listener){

		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_RULES).addParameter("vendor", persona.schoolDomain);

        StringRequest rulesRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                PersonaInfo personaInfo = new PersonaInfo();

                String roles = response.getAsJsonArray("roles").toString();
                if(roles.contains("parent"))
                    MajorObjectsHelper.setPersonaInfoRole(personaInfo, PersonaInfo.Role.PARENT);
                else if(roles.contains("student"))
                    MajorObjectsHelper.setPersonaInfoRole(personaInfo, PersonaInfo.Role.STUDENT);
                else
                    listener.onApiError("Unsupported role");

                MajorObjectsHelper.setPersonaInfoGender(personaInfo, Utility.parseGender(response.get("gender").getAsString()));
                MajorObjectsHelper.setPersonaInfoId(personaInfo, response.get("name").getAsString());
                MajorObjectsHelper.setPersonaInfoEmail(personaInfo, response.get("email").getAsString());
                MajorObjectsHelper.setPersonaInfoName(personaInfo, response.get("firstname").getAsString(), response.get("middlename").getAsString(), response.get("lastname").getAsString());

                JsonObject students = response.getAsJsonObject("relations").getAsJsonObject("students");

                for(Map.Entry<String, JsonElement> entry: students.entrySet()){
                    JsonObject student = entry.getValue().getAsJsonObject();
                    MajorObjectsHelper.addStudentToPersonaInfo(personaInfo, MinorObjectsFactory.createStudent(student.get("title").getAsString(), student.get("name").getAsString(), Utility.parseGender(student.get("gender").getAsString()), student.get("class").getAsString()));
                }

                listener.onSuccess(personaInfo);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

        queue.add(rulesRequest);
        return rulesRequest;
	}

	//Get periods!
	static StringRequest getPeriods(RequestQueue queue, EljurPersona persona, String studentId, final EljurApiClient.JournalismListener<PeriodsInfo> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_PERIODS).addParameter("weeks", "__yes").addParameter("student", studentId);

        StringRequest periodsRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                JsonArray periods = response.getAsJsonArray("students").get(0).getAsJsonObject().getAsJsonArray("periods");
                PeriodsInfo periodsInfo = MajorObjectsFactory.createPeriodsInfo();

                for(JsonElement periodEl: periods){
                    JsonObject period = periodEl.getAsJsonObject();
                    if(period.get("ambigious").getAsBoolean()){
                        MajorObjectsHelper.addAmbigiousPeriodToPeriodsInfo(periodsInfo, MinorObjectsFactory.createAmbigiousPeriod(period.get("name").getAsString(), period.get("fullname").getAsString()));
                    }else {
                        ActualPeriod actualPeriod;

                        try {
                            actualPeriod = MinorObjectsFactory.createActualPeriod(period.get("name").getAsString(), period.get("fullname").getAsString(), period.get("start").getAsString(), period.get("end").getAsString());
                        } catch (EljurApiException e) {
                            listener.onApiError(e.getMessage());
                            return;
                        }

                        JsonArray weeks = period.getAsJsonArray("weeks");

                        for(JsonElement weekEl: weeks){
                            JsonObject week = weekEl.getAsJsonObject();
                            try {
                                MinorObjectsHelper.addWeekToActualPeriod(actualPeriod, MinorObjectsFactory.createWeek(week.get("start").getAsString(), week.get("end").getAsString(), week.get("title").getAsString()));
                            } catch (EljurApiException e) {
                                listener.onApiError(e.getMessage());
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
                listener.onNetworkError();
            }
        });

        queue.add(periodsRequest);

        return periodsRequest;
	}
	
	//Get diary!
	static StringRequest getDiary(RequestQueue queue, EljurPersona persona, final String studentId, String days, boolean getTimes, final EljurApiClient.JournalismListener<DiaryEntry> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_DIARY).addParameter("student", studentId).addParameter("days", days);
        if(getTimes)
            apiRequest.addParameter("rings", "__yes");

        StringRequest diaryRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                ArrayList<WeekDay> weekDays = new ArrayList<WeekDay>();

                if(response.size()==0||response.getAsJsonObject("students").size()==0){
                    listener.onApiError("Расписание отсутствует");
                    return;
                }

                JsonObject weekDaysObj = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonObject("days");
                for(Map.Entry<String, JsonElement> entry: weekDaysObj.entrySet()){
                    JsonObject weekDay = entry.getValue().getAsJsonObject();
                    if(weekDay.get("alert") != null && weekDay.get("alert").getAsString().equals("vacation")){
                        try {
                            weekDays.add(MinorObjectsFactory.createVacationWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString()));
                        } catch (EljurApiException e) {
                            listener.onApiError(e.getMessage());
                            return;
                        }
                        continue;
                    }

                    ArrayList<Lesson> lessons = new ArrayList<Lesson>();
                    for(Map.Entry<String, JsonElement> entry2: weekDay.getAsJsonObject("items").entrySet()){
                        JsonObject lessonObj = entry2.getValue().getAsJsonObject();
                        Lesson lesson = MinorObjectsFactory.createLesson(lessonObj.get("num").getAsString(), lessonObj.get("name").getAsString(), lessonObj.get("room").getAsString(), lessonObj.get("teacher").getAsString());

                        if(lessonObj.get("starttime")!=null&&lessonObj.get("endtime")!=null){
                            try {
                                MinorObjectsHelper.addTimesToLesson(lesson, lessonObj.get("starttime").getAsString(), lessonObj.get("endtime").getAsString());
                            } catch (EljurApiException e) {
                                listener.onApiError(e.getMessage());
                                return;
                            }
                        }

                        Homework homework = null;
                        if(lessonObj.get("homework")!=null){
                            homework = MinorObjectsFactory.createHomework();

                            ArrayList<Hometask> hometasks = new ArrayList<Hometask>();
                            for(Map.Entry<String, JsonElement> entry3: lessonObj.getAsJsonObject("homework").entrySet()){
                                JsonObject hometask = entry3.getValue().getAsJsonObject();
                                hometasks.add(MinorObjectsFactory.createHometask(hometask.get("value").getAsString(), hometask.get("individual").getAsBoolean()));
                            }

                            Collections.sort(hometasks, new HometasksComparator());
                            MinorObjectsHelper.addHometasksToHomework(homework, hometasks);
                        }

                        if(lessonObj.get("files")!=null){
                            if(homework==null)
                                homework = MinorObjectsFactory.createHomework();

                            ArrayList<Attachment> attachments = new ArrayList<Attachment>();
                            for(JsonElement attachmentEl: lessonObj.getAsJsonArray("files")){
                                JsonObject attachment = attachmentEl.getAsJsonObject();
                                attachments.add(MinorObjectsFactory.createAttacment(attachment.get("filename").getAsString(), attachment.get("link").getAsString()));
                            }
                            MinorObjectsHelper.addAttachmentsToHomework(homework, attachments);;
                        }

                        if(homework!=null){
                            MinorObjectsHelper.addHomeworkToLesson(lesson, homework);
                        }

                        if(lessonObj.get("assessments")!=null){
                            ArrayList<Mark> marks = new ArrayList<Mark>();
                            for(JsonElement markEl: lessonObj.getAsJsonArray("assessments")){
                                JsonObject mark = markEl.getAsJsonObject();

                                if(mark.get("comment")!=null&&mark.get("comment").getAsString().length()>0)
                                    marks.add(MinorObjectsFactory.createMarkWithComment(mark.get("value").getAsString(), mark.get("comment").getAsString()));
                                else
                                    marks.add(MinorObjectsFactory.createMark(mark.get("value").getAsString()));
                            }
                            MinorObjectsHelper.addMarksToLesson(lesson, marks);
                        }
                        lessons.add(lesson);
                    }
                    try {
                        weekDays.add(MinorObjectsFactory.createWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString(), lessons));
                    } catch (EljurApiException e) {
                        listener.onApiError(e.getMessage());
                        return;
                    }
                }
                Collections.sort(weekDays, new WeekDaysComparator());
                listener.onSuccess(MajorObjectsFactory.createDiaryEntry(weekDays));

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

        queue.add(diaryRequest);
        return diaryRequest;
	}
	
	//Get marks!
	static StringRequest getMarks(RequestQueue queue, EljurPersona persona, final String studentId, String days, final EljurApiClient.JournalismListener<MarksGrid> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MARKS).addParameter("student", studentId).addParameter("days", days);

        StringRequest marksRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                JsonArray lessons = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonArray("lessons");

                ArrayList<SubjectInGrid> subjects = new ArrayList<SubjectInGrid>();

                for(JsonElement lessonEl: lessons){
                    JsonObject lesson = lessonEl.getAsJsonObject();

                    ArrayList<GridMark> marks = new ArrayList<GridMark>();

                    if(lesson.get("marks")!=null&&lesson.getAsJsonArray("marks").size()>0){
                        for(JsonElement markEl: lesson.getAsJsonArray("marks")){
                            JsonObject mark = markEl.getAsJsonObject();

                            GridMark gridMark;

                            try {
                                if (!mark.get("lesson_comment").isJsonNull()) {
                                    gridMark = MinorObjectsFactory.createGridMarkWithComment(mark.get("value").getAsString(), mark.get("date").getAsString(), mark.get("lesson_comment").getAsString());
                                } else if (mark.get("comment").getAsString().length() > 0) {
                                    gridMark = MinorObjectsFactory.createGridMarkWithComment(mark.get("value").getAsString(), mark.get("date").getAsString(), mark.get("comment").getAsString());
                                } else {
                                    gridMark = MinorObjectsFactory.createGridMark(mark.get("value").getAsString(), mark.get("date").getAsString());
                                }
                            }catch (EljurApiException e){
                                listener.onApiError(e.getMessage());
                                return;
                            }

                            marks.add(gridMark);

                            subjects.add(MinorObjectsFactory.createSubjectInGrid(lesson.get("name").getAsString(), lesson.get("average").getAsString(), marks));
                        }
                    }else {
                        subjects.add(MinorObjectsFactory.createSubjectInGrid(lesson.get("name").getAsString(), lesson.get("average").getAsString(), null));
                    }

                }

                listener.onSuccess(MajorObjectsFactory.createMarksGrid(subjects));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

        queue.add(marksRequest);

		
		return marksRequest;
	}

	//Get schedule!
	static StringRequest getSchedule(RequestQueue queue, EljurPersona persona, final String studentId, String days, boolean getTimes, final EljurApiClient.JournalismListener<Schedule> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_SCHEDULE).addParameter("studentId", studentId).addParameter("days", days);
        if(getTimes)
            apiRequest.addParameter("rings", "__yes");

		StringRequest scheduleRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                JsonObject weekDaysObj = response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonObject("days");

                ArrayList<WeekDay> weekDays = new ArrayList<WeekDay>();
                for(Map.Entry<String, JsonElement> entry: weekDaysObj.entrySet()){
                    JsonObject weekDay = entry.getValue().getAsJsonObject();

                    if(weekDay.get("alert") != null && weekDay.get("alert").getAsString().equals("vacation")){
                        try {
                            weekDays.add(MinorObjectsFactory.createVacationWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString()));
                        } catch (EljurApiException e) {
                            listener.onApiError(e.getMessage());
                            return;
                        }
                        continue;
                    }
                    ArrayList<Lesson> lessons = new ArrayList<Lesson>();
                    for(Map.Entry<String, JsonElement> entry2: weekDay.getAsJsonObject("items").entrySet()){
                        JsonObject lessonObj = entry.getValue().getAsJsonObject();

                        Lesson lesson = MinorObjectsFactory.createLesson(lessonObj.get("num").getAsString(), lessonObj.get("name").getAsString(), lessonObj.get("room").getAsString(), lessonObj.get("teacher").getAsString());

                        if(lessonObj.get("starttime")!=null&&lessonObj.get("endtime")!=null){
                            try {
                                MinorObjectsHelper.addTimesToLesson(lesson, lessonObj.get("starttime").getAsString(), lessonObj.get("endtime").getAsString());
                            } catch (EljurApiException e) {
                                listener.onApiError(e.getMessage());
                                return;
                            }
                        }

                        lessons.add(lesson);
                    }

                    try {
                        weekDays.add(MinorObjectsFactory.createWeekDay(weekDay.get("title").getAsString(), weekDay.get("name").getAsString(), lessons));
                    } catch (EljurApiException e) {
                        listener.onApiError(e.getMessage());
                        return;
                    }
                }

                listener.onSuccess(MajorObjectsFactory.createSchedule(weekDays));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });
		
		queue.add(scheduleRequest);
		return scheduleRequest;
	}
	
	//Get messages list!
	static StringRequest getMessages(RequestQueue queue, EljurPersona persona, final MessagesList.Folder folder, boolean unreadOnly, final EljurApiClient.JournalismListener<MessagesList> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGES).addParameter("folder", folder == MessagesList.Folder.INBOX?"inbox":"sent").addParameter("unreadonly", String.valueOf(unreadOnly));

        StringRequest messagesListRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                ArrayList<ShortMessage> shortMessages = new ArrayList<ShortMessage>();
                int de = 0;
                for(JsonElement messageEl: response.getAsJsonArray("messages")){
                    JsonObject message = messageEl.getAsJsonObject();
                    System.out.println("DEB: "+de);
                    de++;

                    if(folder == MessagesList.Folder.INBOX){
                        JsonObject sender = message.getAsJsonObject("user_from");
                        try {
                            shortMessages.add(MinorObjectsFactory.createInboxShortMessage(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("short_text").getAsString(), message.get("date").getAsString(),MinorObjectsFactory.createMessagePerson(sender.get("name").getAsString(), sender.get("firstname").getAsString(), sender.get("middlename").getAsString(), sender.get("lastname").getAsString()), message.get("unread").getAsBoolean(), message.get("with_files").getAsBoolean(), message.get("with_resources").getAsBoolean()));
                        } catch (EljurApiException e) {
                            listener.onApiError(e.getMessage());
                            return;
                        }
                    }else {
                        if(message.get("users_to")==null)
                            continue;

                        ArrayList<MessagePerson> receivers = new ArrayList<MessagePerson>();

                        for(JsonElement receiverEl: message.getAsJsonArray("users_to")){

                            JsonObject receiver = receiverEl.getAsJsonObject();
                            receivers.add(MinorObjectsFactory.createMessagePerson(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString()));
                        }
                        try {
                            shortMessages.add(MinorObjectsFactory.createSentShortMessage(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("short_text").getAsString(), message.get("date").getAsString(),receivers, message.get("unread").getAsBoolean(), message.get("with_files").getAsBoolean(), message.get("with_resources").getAsBoolean()));
                        } catch (EljurApiException e) {
                            listener.onApiError(e.getMessage());
                            return;
                        }
                    }
                }

                listener.onSuccess(MajorObjectsFactory.createMessagesList(response.get("total").getAsInt(), response.get("count").getAsInt(), shortMessages));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

        queue.add(messagesListRequest);
        return messagesListRequest;
	}

	//Get message info!
	public static StringRequest getMessageInfo(RequestQueue queue, EljurPersona persona, final MessagesList.Folder folder, String messageId, final EljurApiClient.JournalismListener<MessageInfo> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGE_INFO).addParameter("id", messageId);

        StringRequest messageInfoRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject message = Utility.getJsonFromResponse(rawResponse).getAsJsonObject("message");

                ArrayList<MessagePerson> receivers = new ArrayList<MessagePerson>();
                for(JsonElement receiverEl: message.getAsJsonArray("user_to")){

                    JsonObject receiver = receiverEl.getAsJsonObject();
                    receivers.add(MinorObjectsFactory.createMessagePerson(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString()));
                }


                JsonObject sender = message.getAsJsonObject("user_from");
                MessageInfo messageInfo = null;
                try {
                    messageInfo = MinorObjectsFactory.createMessageInfo(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("text").getAsString(), message.get("date").getAsString(), folder, MinorObjectsFactory.createMessagePerson(sender.get("name").getAsString(), sender.get("firstname").getAsString(), sender.get("middlename").getAsString(), sender.get("lastname").getAsString()), receivers);
                } catch (EljurApiException e) {
                    listener.onApiError(e.getMessage());
                    return;
                }

                if(message.get("files")!=null){
                    ArrayList<Attachment> attachments = new ArrayList<Attachment>();
                    for(JsonElement fileEl: message.getAsJsonArray("files")){
                        JsonObject file = fileEl.getAsJsonObject();
                        attachments.add(MinorObjectsFactory.createAttacment(file.get("filename").getAsString(), file.get("link").getAsString()));
                    }

                    MinorObjectsHelper.addAttacmentsToMessageInfo(messageInfo, attachments);
                }

                listener.onSuccess(messageInfo);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

        queue.add(messageInfoRequest);
		return messageInfoRequest;
	}
	
	public static StringRequest getMessageReceivers(RequestQueue queue, EljurPersona persona, final EljurApiClient.JournalismListener<MessageReceiversInfo> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_MESSAGE_RECEIVERS);

        StringRequest messageReceiversInfoRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                ArrayList<MessageReceiversGroup> groups = new ArrayList<MessageReceiversGroup>();

                for(JsonElement groupEl: response.getAsJsonArray("groups")){
                    JsonObject group = groupEl.getAsJsonObject();

                    ArrayList<MessageReceiver> receivers = new ArrayList<MessageReceiver>();
                    for(JsonElement receiverEl: group.getAsJsonArray("users")){
                        JsonObject receiver = receiverEl.getAsJsonObject();

                        receivers.add(MinorObjectsFactory.createMessageReceiver(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString(), (receiver.get("info")!=null?receiver.get("info").getAsString():null)));
                    }
                    groups.add(MinorObjectsFactory.createMessageReceiversGroup(group.get("key").getAsString(), group.get("name").getAsString(), receivers));
                }

                listener.onSuccess(MajorObjectsFactory.createMessageReceiversInfo(groups));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

		queue.add(messageReceiversInfoRequest);
		return messageReceiversInfoRequest;
	}
	
	public static StringRequest sendMessage(RequestQueue queue, EljurPersona persona, String subject, String text, ArrayList<MessageReceiver> receivers, final EljurApiClient.JournalismListener<SentMessageResponse> listener){
		final StringBuilder r = new StringBuilder();
        for(MessageReceiver receiver:receivers){
            r.append(receiver.getId()+",");
        }
        
        EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.SEND_MESSAGE).addParameter("users_to", r.substring(0,r.length()-1)).addParameter("subject", subject).addParameter("text", text);

        StringRequest sendMessageRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getRawJsonFromResponse(rawResponse);

                if(response.get("state").getAsInt()==200&&response.get("error").isJsonNull())
                    listener.onSuccess(MajorObjectsFactory.createSentMessageResponse(true));
                else
                    listener.onApiError("Message was not sent!");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
            }
        });

        queue.add(sendMessageRequest);
        return sendMessageRequest;
	}
	
	public static StringRequest getFinals(RequestQueue queue, EljurPersona persona, final String studentId, final EljurApiClient.JournalismListener<Finals> listener){
		EljurApiRequest apiRequest = new EljurApiRequest(persona, EljurApiRequest.GET_FINALS).addParameter("student", studentId);

        StringRequest finalsRequest = new StringRequest(Request.Method.GET, apiRequest.getRequestURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String rawResponse) {
                JsonObject response = Utility.getJsonFromResponse(rawResponse);

                ArrayList<FinalSubject> subjects = new ArrayList<FinalSubject>();

                for(JsonElement subjectEl: response.getAsJsonObject("students").getAsJsonObject(studentId).getAsJsonArray("items")){
                    JsonObject subject = subjectEl.getAsJsonObject();

                    if(subject.get("assessments")!=null&&subject.getAsJsonArray("assessments").size()>0){
                        ArrayList<FinalPeriod> periods = new ArrayList<FinalPeriod>();

                        for(JsonElement periodEl: subject.getAsJsonArray("assessments")){
                            JsonObject period = periodEl.getAsJsonObject();

                            periods.add(MinorObjectsFactory.createFinalPeriod(period.get("period").getAsString(), period.get("value").getAsString(), (period.get("comment")!=null&&period.get("comment").getAsString().length()>0)?period.get("comment").getAsString():null));
                        }

                        subjects.add(MinorObjectsFactory.createFinalSubject(subject.get("name").getAsString(), periods));
                    }else {
                        subjects.add(MinorObjectsFactory.createFinalSubject(subject.get("name").getAsString(), null));
                    }
                }

                listener.onSuccess(MajorObjectsFactory.createFinals(subjects));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onNetworkError();
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
