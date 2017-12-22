package com.aefyr.journalism.parsing;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.Utility;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MajorObjectsFactory;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.MessagePerson;
import com.aefyr.journalism.objects.minor.MinorObjectsFactory;
import com.aefyr.journalism.objects.minor.ShortMessage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

/**
 * Created by Aefyr on 16.08.2017.
 */

public class MessagesListAsyncParser {
    private static MessagesListAsyncParser instance;

    private MessagesListAsyncParser() {
        instance = this;
    }

    public static MessagesListAsyncParser getInstance() {
        return instance == null ? new MessagesListAsyncParser() : instance;
    }

    public void parseMessages(String rawMessages, MessagesList.Folder folder, EljurApiClient.JournalismListener<MessagesList> listener) {
        new MessagesParseTask().execute(new MessagesParseTaskParams(rawMessages, folder, listener));
    }

    private class MessagesParseTaskParams extends AsyncParserParams<MessagesList> {
        private MessagesList.Folder folder;

        private MessagesParseTaskParams(String rawResponse, MessagesList.Folder folder, EljurApiClient.JournalismListener<MessagesList> listener) {
            super(rawResponse, null, listener);
            this.folder = folder;
        }
    }

    private class MessagesParseTask extends AsyncParserBase<MessagesList> {

        @Override
        protected AsyncParserTaskResult<MessagesList> doInBackground(AsyncParserParams<MessagesList>... asyncParserParamses) {
            MessagesParseTaskParams messagesParseTaskParams = (MessagesParseTaskParams) asyncParserParamses[0];

            bindJournalismListener(messagesParseTaskParams.listener);
            String rawResponse = messagesParseTaskParams.rawResponse;
            JsonObject response = Utility.getJsonFromResponse(rawResponse);
            MessagesList.Folder folder = messagesParseTaskParams.folder;

            if (response == null || response.size() == 0 || response.get("messages") == null) {
                return new AsyncParserTaskResult<MessagesList>(MajorObjectsFactory.createMessagesList(0, 0, new ArrayList<ShortMessage>(0), folder));
            }

            JsonArray jShortMessages = response.getAsJsonArray("messages");
            ArrayList<ShortMessage> shortMessages = new ArrayList<>(jShortMessages.size());
            for (JsonElement messageEl : jShortMessages) {
                JsonObject message = messageEl.getAsJsonObject();

                if (folder == MessagesList.Folder.INBOX) {
                    JsonObject sender = message.getAsJsonObject("user_from");
                    try {
                        shortMessages.add(MinorObjectsFactory.createInboxShortMessage(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("short_text").getAsString(), message.get("date").getAsString(), MinorObjectsFactory.createMessagePerson(sender.get("name").getAsString(), sender.get("firstname").getAsString(), sender.get("middlename").getAsString(), sender.get("lastname").getAsString()), message.get("unread").getAsBoolean(), message.get("with_files").getAsBoolean(), message.get("with_resources").getAsBoolean()));
                    } catch (JournalismException e) {
                        return new AsyncParserTaskResult<MessagesList>(e);
                    }
                } else {
                    //Eljur sure likes phantoms ^^
                    //Phantom receivers list filter
                    if (message.get("users_to") == null)
                        continue;

                    JsonArray jReceivers = message.getAsJsonArray("users_to");
                    ArrayList<MessagePerson> receivers = new ArrayList<>(jReceivers.size());

                    for (JsonElement receiverEl : jReceivers) {

                        JsonObject receiver = receiverEl.getAsJsonObject();
                        receivers.add(MinorObjectsFactory.createMessagePerson(receiver.get("name").getAsString(), receiver.get("firstname").getAsString(), receiver.get("middlename").getAsString(), receiver.get("lastname").getAsString()));
                    }
                    try {
                        shortMessages.add(MinorObjectsFactory.createSentShortMessage(message.get("id").getAsString(), message.get("subject").getAsString(), message.get("short_text").getAsString(), message.get("date").getAsString(), receivers, message.get("unread").getAsBoolean(), message.get("with_files").getAsBoolean(), message.get("with_resources").getAsBoolean()));
                    } catch (JournalismException e) {
                        return new AsyncParserTaskResult<MessagesList>(e);
                    }
                }
            }

            return new AsyncParserTaskResult<MessagesList>(MajorObjectsFactory.createMessagesList(response.get("total").getAsInt(), response.get("count").getAsInt(), shortMessages, folder));
        }
    }

}
