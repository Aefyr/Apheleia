package com.aefyr.apheleia.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.MessageViewActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.MessagesAdapter;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.MessagesList;
import com.android.volley.toolbox.StringRequest;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessagesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MessagesAdapter.OnMessageClickListener {

    private boolean firstLoad;
    private StringRequest currentRequest;
    private View emptyMessages;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView messagesRecycler;
    private MessagesAdapter messagesAdapter;

    private EljurPersona persona;
    private EljurApiClient apiClient;
    private ConnectionHelper connectionHelper;

    private static MessagesList.Folder currentFolder;

    public MessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(this);
        messagesRecycler = (RecyclerView) view.findViewById(R.id.messagesRecycler);
        messagesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        emptyMessages = view.findViewById(R.id.emptyMessages);

        persona = Helper.getInstance(getActivity()).getPersona();
        apiClient = EljurApiClient.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());


        if(currentFolder==null)
            currentFolder = MessagesList.Folder.INBOX;


        loadMessages(currentFolder);
        return view;
    }


    public boolean isInboxSelected(){
        return currentFolder == MessagesList.Folder.INBOX;
    }


    private void setMessagesToAdapter(MessagesList messagesList){
        if(messagesAdapter==null){
            messagesAdapter = new MessagesAdapter(getActivity(), messagesList.getMessages(), this);
            messagesAdapter.setHasStableIds(true);
            messagesRecycler.setAdapter(messagesAdapter);
        }else
            messagesAdapter.setMessages(messagesList.getMessages());
    }

    private boolean loadedFromMemory;
    private void loadMessages(MessagesList.Folder folder){
        refreshLayout.setRefreshing(true);

        if(firstLoad||!connectionHelper.hasNetworkConnection()){
            //Try loading cached version
            //Wait, should we even cache messages? Someone may have thousands of them, and deserializing 3000 messages is not such a good idea, is it?
        }

        if(loadedFromMemory){
            //Maybe do something
        }

        currentRequest = apiClient.getMessages(persona, folder, false, new EljurApiClient.JournalismListener<MessagesList>() {
            @Override
            public void onSuccess(MessagesList result) {

                setMessagesToAdapter(result);
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError() {
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(String message, String json) {
                refreshLayout.setRefreshing(false);
            }
        });
    }

    public void toggleFolder(){
        if(!currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
        if(currentFolder == MessagesList.Folder.INBOX) {
            currentFolder = MessagesList.Folder.SENT;
            Chief.makeAToast(getActivity(), getString(R.string.sent));
        }else {
            currentFolder = MessagesList.Folder.INBOX;
            Chief.makeAToast(getActivity(), getString(R.string.inbox));
        }
        loadMessages(currentFolder);
    }

    @Override
    public void onRefresh() {
        loadMessages(currentFolder);
    }

    @Override
    public void onMessageClick(String messageId, boolean inbox) {
        Intent messageViewIntent = new Intent(getActivity(), MessageViewActivity.class);
        messageViewIntent.putExtra("messageId", messageId);
        messageViewIntent.putExtra("inbox", inbox);
        startActivity(messageViewIntent);
    }

    @Override
    public void onDetach() {
        cancelRequest();
        super.onDetach();
    }

    private void cancelRequest(){
        if(currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void checkEmptiness(MessagesList list){
        if(list.getMessages().size()==0)
            emptyMessages.setVisibility(View.VISIBLE);
        else
            emptyMessages.setVisibility(View.GONE);
    }

}
