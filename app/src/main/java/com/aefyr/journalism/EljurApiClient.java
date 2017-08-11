package com.aefyr.journalism;



import android.content.Context;

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
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;


public class EljurApiClient {

    public interface JournalismListener<T>{
        void onSuccess(T result);
        void onNetworkError();
        void onApiError(String message);
    }

    public interface LoginRequestListener{
        void onSuccessfulLogin(Token token);
        void onInvalidCredentialsError();
        void onInvalidDomainError();
        void onNetworkError();
        void onApiError(String message);
    }

    private static EljurApiClient instance;
	private RequestQueue queue;

	private EljurApiClient(Context c){
		queue = Volley.newRequestQueue(c);
        instance = this;
	}

	public static EljurApiClient getInstance(Context c){
        return instance==null?new EljurApiClient(c):instance;
    }


	
	public StringRequest requestToken(String schoolDomain, String username, String password, LoginRequestListener listener){
		return EljurApiRequests.loginRequest(queue, schoolDomain, username, password, listener);
	}

	public StringRequest getRules(EljurPersona persona, JournalismListener<PersonaInfo> listener){
		return EljurApiRequests.getRules(queue, persona, listener);
	}
	
	public StringRequest getPeriods(EljurPersona persona, String studentId, JournalismListener<PeriodsInfo> listener){
		return EljurApiRequests.getPeriods(queue, persona, studentId, listener);
	}
	
	public StringRequest getDiary(EljurPersona persona, String studentId, String days, JournalismListener<DiaryEntry> listener){
		return EljurApiRequests.getDiary(queue, persona, studentId, days, listener);
	}
	
	public StringRequest getMarks(EljurPersona persona, String studentId, String days, JournalismListener<MarksGrid> listener){
		return EljurApiRequests.getMarks(queue, persona, studentId, days, listener);
	}
	
	public StringRequest getSchedule(EljurPersona persona, String studentId, String days, JournalismListener<Schedule> listener){
		return EljurApiRequests.getSchedule(queue, persona, studentId, days, listener);
	}
	
	public StringRequest getMessages(EljurPersona persona, MessagesList.Folder folder, boolean unreadOnly, JournalismListener<MessagesList> listener){
		return EljurApiRequests.getMessages(queue, persona, folder, unreadOnly, listener);
	}
	
	public StringRequest getMessageInfo(EljurPersona persona, MessagesList.Folder folder, String messageId, JournalismListener<MessageInfo> listener){
		return EljurApiRequests.getMessageInfo(queue, persona, folder, messageId, listener);
	}
	
	public StringRequest getMessageInfoFromMessage(EljurPersona persona, ShortMessage message, JournalismListener<MessageInfo> listener){
		return EljurApiRequests.getMessageInfo(queue, persona, message.getFolder(), message.getId(), listener);
	}
	
	public StringRequest getMessagesReceivers(EljurPersona persona, JournalismListener<MessageReceiversInfo> listener){
		return EljurApiRequests.getMessageReceivers(queue, persona, listener);
	}
	
	public StringRequest sendMessage(EljurPersona persona, String subject, String text, ArrayList<MessageReceiver> receivers, JournalismListener<SentMessageResponse> listener){
		return EljurApiRequests.sendMessage(queue, persona, subject, text, receivers, listener);
	}
	
	public StringRequest getFinals(EljurPersona persona, String studentId, JournalismListener<Finals> listener){
		return EljurApiRequests.getFinals(queue, persona, studentId, listener);
	}
}
