package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.Utility;
import com.aefyr.journalism.exceptions.EljurApiException;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.Attachment;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.aefyr.journalism.objects.minor.MessagePerson;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.MinorObjectsHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class MessageInfoAsyncParser {
    private static MessageInfoAsyncParser instance;

    private MessageInfoAsyncParser(){
        instance = this;
    }

    public static MessageInfoAsyncParser getInstance(){
        return instance == null?new MessageInfoAsyncParser():instance;
    }

    public void parseMessage(String rawMessage, MessagesList.Folder folder, EljurApiClient.JournalismListener<MessageInfo> listener){
        new MessageInfoParseTask().execute(new AsyncParserParams<MessageInfo>(rawMessage, folder== MessagesList.Folder.INBOX?"i":"s", listener));
    }

    private class MessageInfoParseTask extends AsyncParserBase<MessageInfo> {
        @Override
        protected AsyncParserTaskResult<MessageInfo> doInBackground(AsyncParserParams<MessageInfo>... params) {
            bindJournalismListener(params[0].listener);
            String rawResponse = params[0].rawResponse;
            MessagesList.Folder folder = params[0].journalismParam.equals("i") ? MessagesList.Folder.INBOX : MessagesList.Folder.SENT;

            JsonObject message = Utility.getJsonFromResponse(rawResponse).getAsJsonObject("message");

            ArrayList<MessagePerson> receivers = new ArrayList<MessagePerson>();
            for (JsonElement receiverEl : message.getAsJsonArray("user_to")) {

                JsonObject receiver = receiverEl.getAsJsonObject();
                receivers.add(MinorObjectsFactory.createMessagePerson(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString()));
            }


            JsonObject sender = message.getAsJsonObject("user_from");
            MessageInfo messageInfo;
            try {
                messageInfo = MinorObjectsFactory.createMessageInfo(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("text").getAsString(), message.get("date").getAsString(), folder, MinorObjectsFactory.createMessagePerson(sender.get("name").getAsString(), sender.get("firstname").getAsString(), sender.get("middlename").getAsString(), sender.get("lastname").getAsString()), receivers);
            } catch (EljurApiException e) {
                return new AsyncParserTaskResult<>(e.getMessage(), rawResponse);
            }

            if (message.get("files") != null) {
                ArrayList<Attachment> attachments = new ArrayList<Attachment>();
                for (JsonElement fileEl : message.getAsJsonArray("files")) {
                    JsonObject file = fileEl.getAsJsonObject();
                    attachments.add(MinorObjectsFactory.createAttacment(file.get("filename").getAsString(), file.get("link").getAsString()));
                }

                MinorObjectsHelper.addAttacmentsToMessageInfo(messageInfo, attachments);
            }

            return new AsyncParserTaskResult<>(messageInfo);
        }
    }
}