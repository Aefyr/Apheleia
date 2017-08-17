package com.aefyr.apheleia.fragments;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.MessageViewActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.Utility;
import com.aefyr.apheleia.adapters.MessagesAdapter;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.MessagesHelper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.MessagesList;
import com.android.volley.toolbox.StringRequest;

import java.util.HashSet;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessagesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MessagesAdapter.OnMessageClickListener {

    private boolean firstLoad = true;
    private StringRequest currentRequest;
    private View emptyMessages;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView messagesRecycler;
    private MessagesAdapter messagesAdapter;
    private FloatingActionButton composeFab;

    private EljurPersona persona;
    private EljurApiClient apiClient;
    private ConnectionHelper connectionHelper;
    private MessagesHelper messagesHelper;

    private static MessagesList.Folder currentFolder;
    private HashSet<AsyncTask> tasks;


    public MessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(this);
        composeFab = (FloatingActionButton) view.findViewById(R.id.composeFab);
        messagesRecycler = (RecyclerView) view.findViewById(R.id.messagesRecycler);
        messagesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        DividerItemDecoration divider = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.messages_divider));
        messagesRecycler.addItemDecoration(divider);
        emptyMessages = view.findViewById(R.id.emptyMessages);
        initializeFabVisibilitySwitcher();

        persona = Helper.getInstance(getActivity()).getPersona();
        apiClient = EljurApiClient.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());
        messagesHelper = MessagesHelper.getInstance(getActivity());
        tasks = new HashSet<>();


        if(currentFolder==null)
            currentFolder = MessagesList.Folder.INBOX;


        loadMessages(currentFolder);
        return view;
    }

    private static final int COMPOSE_FAB_VISIBILITY_CHANGE_THRESHOLD_IN_DP = 8;
    private int fabVisibilityChangeThreshold;
    private int fabVisibilitySwitcherProgress;
    private boolean composeFabShown = true;
    private void initializeFabVisibilitySwitcher(){
        fabVisibilityChangeThreshold = (int) Utility.dpToPx(COMPOSE_FAB_VISIBILITY_CHANGE_THRESHOLD_IN_DP, getResources());

        messagesRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(Math.abs(fabVisibilitySwitcherProgress)<= fabVisibilityChangeThreshold){
                    fabVisibilitySwitcherProgress = Utility.clamp(fabVisibilitySwitcherProgress +dy, -fabVisibilityChangeThreshold, fabVisibilityChangeThreshold);
                }

                if(fabVisibilitySwitcherProgress >= fabVisibilityChangeThreshold) {
                    if(composeFabShown){
                        composeFab.hide();
                        composeFabShown = false;
                    }
                }else if(fabVisibilitySwitcherProgress <=-fabVisibilityChangeThreshold){
                    if(!composeFabShown){
                        composeFab.show();
                        composeFabShown = true;
                    }
                }
            }
        });
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
        checkEmptiness(messagesList);
    }

    private void loadMessages(final MessagesList.Folder folder){
        refreshLayout.setRefreshing(true);

        if(folderToggled||firstLoad||!connectionHelper.hasNetworkConnection()){
            tasks.add(messagesHelper.loadMessages(folder == MessagesList.Folder.INBOX, new MessagesHelper.LoadMessagesTaskResultListener() {
                @Override
                public void onSuccess(MessagesList list) {
                    setMessagesToAdapter(list);
                    System.out.println("Loaded messages");
                }

                @Override
                public void onFail() {
                    System.out.println("Failed to load messages");
                }
            }));

            firstLoad = false;
            folderToggled = false;
        }

        currentRequest = apiClient.getMessages(persona, folder, false, new EljurApiClient.JournalismListener<MessagesList>() {
            @Override
            public void onSuccess(MessagesList result) {
                System.out.println("Set messages!");
                messagesHelper.saveMessages(result, folder == MessagesList.Folder.INBOX, null);
                setMessagesToAdapter(result);
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError() {
                refreshLayout.setRefreshing(false);
                Chief.makeASnack(getView(), getString(R.string.network_error_tip));
            }

            @Override
            public void onApiError(String message, String json) {
                refreshLayout.setRefreshing(false);
                Chief.makeReportApiErrorDialog(getActivity(), getString(R.string.messages), message, json, false);
            }
        });
    }

    private boolean folderToggled;
    public void toggleFolder(){
        if(!currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
        if(currentFolder == MessagesList.Folder.INBOX) {
            currentFolder = MessagesList.Folder.SENT;
            Chief.makeAFlyingToast(getActivity(), getString(R.string.sent));
        }else {
            currentFolder = MessagesList.Folder.INBOX;
            Chief.makeAFlyingToast(getActivity(), getString(R.string.inbox));
        }
        folderToggled = true;
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
        cancelTasks();
        super.onDetach();
    }

    private void cancelRequest(){
        if(currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void cancelTasks(){
        for(AsyncTask task: tasks)
            task.cancel(true);
        tasks.clear();
    }

    private void checkEmptiness(MessagesList list){
        if(list.getMessages().size()==0)
            emptyMessages.setVisibility(View.VISIBLE);
        else
            emptyMessages.setVisibility(View.GONE);
    }

}
