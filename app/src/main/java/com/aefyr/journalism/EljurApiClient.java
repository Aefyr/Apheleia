package com.aefyr.journalism;


import android.content.Context;

import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.major.Finals;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.aefyr.journalism.objects.major.MessageReceiversInfo;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.major.PeriodsInfo;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.major.Schedule;
import com.aefyr.journalism.objects.major.SentMessageResponse;
import com.aefyr.journalism.objects.major.Token;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.aefyr.journalism.objects.minor.MessageReceiver;
import com.aefyr.journalism.objects.minor.ShortMessage;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashSet;


public class EljurApiClient {

    public interface JournalismListener<T> {
        void onSuccess(T result);

        void onNetworkError(boolean tokenIsWrong);

        void onApiError(JournalismException e);
    }

    public interface LoginRequestListener {
        void onSuccessfulLogin(Token token);

        void onInvalidCredentialsError();

        void onInvalidDomainError();

        void onNetworkError();

        void onApiError(JournalismException e);

        void onApiAccessForbidden();
    }

    private static EljurApiClient instance;
    private RequestQueue queue;

    private EljurApiClient(Context c) {
        queue = Volley.newRequestQueue(c);
        instance = this;
    }

    public static EljurApiClient getInstance(Context c) {
        return instance == null ? new EljurApiClient(c) : instance;
    }


    public Request requestToken(String schoolDomain, String username, String password, LoginRequestListener listener) {
        return EljurApiRequests.loginRequest(queue, schoolDomain, username, password, listener);
    }

    public Request getRules(EljurPersona persona, JournalismListener<PersonaInfo> listener) {
        return EljurApiRequests.getRules(queue, persona, listener);
    }

    public Request getPeriods(EljurPersona persona, String studentId, JournalismListener<PeriodsInfo> listener) {
        return EljurApiRequests.getPeriods(queue, persona, studentId, listener);
    }

    public Request getDiary(EljurPersona persona, String studentId, String days, boolean getTimes, JournalismListener<DiaryEntry> listener) {
        return EljurApiRequests.getDiary(queue, persona, studentId, days, getTimes, listener);
    }

    public Request getMarks(EljurPersona persona, String studentId, String days, JournalismListener<MarksGrid> listener) {
        return EljurApiRequests.getMarks(queue, persona, studentId, days, listener);
    }

    public Request getSchedule(EljurPersona persona, String studentId, String days, boolean getTimes, JournalismListener<Schedule> listener) {
        return EljurApiRequests.getSchedule(queue, persona, studentId, days, getTimes, listener);
    }

    public Request getMessages(EljurPersona persona, MessagesList.Folder folder, boolean unreadOnly, JournalismListener<MessagesList> listener) {
        return EljurApiRequests.getMessages(queue, persona, folder, unreadOnly, listener);
    }

    public Request getMessageInfo(EljurPersona persona, MessagesList.Folder folder, String messageId, int numberOfReceiversToParse, JournalismListener<MessageInfo> listener) {
        return EljurApiRequests.getMessageInfo(queue, persona, folder, messageId, numberOfReceiversToParse, listener);
    }

    public Request getMessageInfoFromMessage(EljurPersona persona, ShortMessage message, int numberOfReceiversToParse, JournalismListener<MessageInfo> listener) {
        return EljurApiRequests.getMessageInfo(queue, persona, message.getFolder(), message.getId(), numberOfReceiversToParse, listener);
    }

    public Request getMessagesReceivers(EljurPersona persona, JournalismListener<MessageReceiversInfo> listener) {
        return EljurApiRequests.getMessageReceivers(queue, persona, listener);
    }

    public Request sendMessage(EljurPersona persona, String subject, String text, ArrayList<MessageReceiver> receivers, JournalismListener<SentMessageResponse> listener) {
        HashSet<String> receiversIds = new HashSet<>();
        for (MessageReceiver receiver : receivers) {
            receiversIds.add(receiver.getId());
        }
        return EljurApiRequests.sendMessage(queue, persona, subject, text, receiversIds, listener);
    }

    public Request sendMessage(EljurPersona persona, String subject, String text, HashSet<String> receiversIds, JournalismListener<SentMessageResponse> listener) {
        return EljurApiRequests.sendMessage(queue, persona, subject, text, receiversIds, listener);
    }

    public Request getFinals(EljurPersona persona, String studentId, JournalismListener<Finals> listener) {
        return EljurApiRequests.getFinals(queue, persona, studentId, listener);
    }

    //Not that easy, apparently...
    public Request hijackFCM(EljurPersona persona, String fcmToken, JournalismListener<Boolean> listener) {
        return EljurApiRequests.hijackFCM(queue, persona, fcmToken, listener);
    }
}
