package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.Utility;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.Attachment;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.aefyr.journalism.objects.minor.MessagePerson;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.MinorObjectsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class MessageInfoAsyncParser {
    private static MessageInfoAsyncParser instance;

    private MessageInfoAsyncParser() {
        instance = this;
    }

    public static MessageInfoAsyncParser getInstance() {
        return instance == null ? new MessageInfoAsyncParser() : instance;
    }

    public static final int ALL_RECEIVERS = -1;

    public void parseMessage(String rawMessage, MessagesList.Folder folder, int parsedReceiversCount, EljurApiClient.JournalismListener<MessageInfo> listener) {
        new MessageInfoParseTask(rawMessage, parsedReceiversCount, folder, listener).execute();
    }

    private class MessageInfoParseTask extends AsyncParserBase<MessageInfo> {
        private String rawResponse;
        private int parsedReceiversCount;
        private MessagesList.Folder folder;

        MessageInfoParseTask(String rawResponse, int parsedReceiversCount, MessagesList.Folder folder, EljurApiClient.JournalismListener<MessageInfo> listener){
            bindJournalismListener(listener);
            this.rawResponse = rawResponse;
            this.parsedReceiversCount = parsedReceiversCount;
            this.folder = folder;
        }

        @Override
        protected AsyncParserTaskResult<MessageInfo> doInBackground(Void... voids) {
            JsonObject message = Utility.getJsonFromResponse(rawResponse).getAsJsonObject("message");

            JsonArray jReceivers = message.getAsJsonArray("user_to");

            if(parsedReceiversCount==ALL_RECEIVERS)
                parsedReceiversCount = jReceivers.size();
            else
                parsedReceiversCount = parsedReceiversCount>jReceivers.size()?jReceivers.size():parsedReceiversCount;

            ArrayList<MessagePerson> receivers = new ArrayList<MessagePerson>(parsedReceiversCount);
            for(int i = 0; i<parsedReceiversCount; i++){
                JsonObject receiver = jReceivers.get(i).getAsJsonObject();
                receivers.add(MinorObjectsFactory.createMessagePerson(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString()));
            }

            JsonObject sender = message.getAsJsonObject("user_from");
            MessageInfo messageInfo;
            try {
                messageInfo = MinorObjectsFactory.createMessageInfo(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("text").getAsString(), message.get("date").getAsString(), folder, MinorObjectsFactory.createMessagePerson(sender.get("name").getAsString(), sender.get("firstname").getAsString(), sender.get("middlename").getAsString(), sender.get("lastname").getAsString()), receivers, jReceivers.size());
            } catch (JournalismException e) {
                return new AsyncParserTaskResult<>(e);
            }

            if (message.get("files") != null) {
                JsonArray jFiles = message.getAsJsonArray("files");
                ArrayList<Attachment> attachments = new ArrayList<Attachment>(jFiles.size());
                for (JsonElement fileEl : jFiles) {
                    JsonObject file = fileEl.getAsJsonObject();
                    attachments.add(MinorObjectsFactory.createAttacment(file.get("filename").getAsString(), file.get("link").getAsString()));
                }

                MinorObjectsHelper.addAttacmentsToMessageInfo(messageInfo, attachments);
            }

            return new AsyncParserTaskResult<>(messageInfo);
        }
    }
}
