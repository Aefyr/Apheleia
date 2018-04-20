package com.aefyr.apheleia.fragments;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.ActionListener;
import com.aefyr.apheleia.LoginActivity;
import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.MessageComposeActivity;
import com.aefyr.apheleia.MessageViewActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.MessagesAdapter;
import com.aefyr.apheleia.helpers.AnalyticsHelper;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.apheleia.viewmodels.messages.MessagesListState;
import com.aefyr.apheleia.viewmodels.messages.MessagesLiveData;
import com.aefyr.apheleia.viewmodels.messages.MessagesViewModel;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.ShortMessage;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessagesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MessagesAdapter.OnMessageClickListener, ActionListener {
    private View emptyMessages;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView messagesRecycler;
    private MessagesAdapter messagesAdapter;
    private FloatingActionButton composeFab;


    private static MessagesList.Folder currentFolder;
    private MessagesViewModel viewModel;

    public MessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        updateActionBarTitle();

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        Utility.colorRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        composeFab = (FloatingActionButton) view.findViewById(R.id.composeFab);
        messagesRecycler = (RecyclerView) view.findViewById(R.id.messagesRecycler);
        messagesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        DividerItemDecoration divider = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.messages_divider));
        messagesRecycler.addItemDecoration(divider);
        emptyMessages = view.findViewById(R.id.emptyMessages);
        initializeFab();

        if (currentFolder == null)
            currentFolder = MessagesList.Folder.INBOX;

        viewModel = ViewModelProviders.of(this).get(MessagesViewModel.class);
        MessagesLiveData liveData = viewModel.getMessagesLiveData();
        if (liveData.getValue().getData() != null) {
            setMessagesToAdapter(liveData.getValue().getData());
        }

        liveData.observe(this, new Observer<MessagesListState>() {
            @Override
            public void onChanged(@Nullable MessagesListState messagesListState) {
                Log.d("Messages", "LiveData state=" + messagesListState.getState());

                switch (messagesListState.getState()) {
                    case MessagesListState.NOT_READY:
                        refreshLayout.setRefreshing(true);
                        break;
                    case MessagesListState.OK_CACHED_PRELOAD:
                        setMessagesToAdapter(messagesListState.getData());
                        break;
                    case MessagesListState.OK:
                        refreshLayout.setRefreshing(false);
                        setMessagesToAdapter(messagesListState.getData());
                        break;
                    case MessagesListState.UPDATING:
                        refreshLayout.setRefreshing(true);
                        break;
                    case MessagesListState.NET_ERROR:
                        refreshLayout.setRefreshing(false);
                        Chief.makeASnack(getView(), getString(R.string.offline_mode));
                        break;
                    case MessagesListState.API_ERROR:
                        refreshLayout.setRefreshing(false);
                        Crashlytics.logException(messagesListState.getApiErrorInfo());
                        Chief.makeApiErrorAlert(getActivity(), false);
                        break;
                    case MessagesListState.TOKEN_DEAD:
                        refreshLayout.setRefreshing(false);
                        LoginActivity.tokenExpired(getActivity());
                        break;
                }
            }
        });

        return view;
    }


    private static final int COMPOSE_FAB_VISIBILITY_CHANGE_THRESHOLD_IN_DP = 4;
    private int fabVisibilityChangeThreshold;
    private int fabVisibilitySwitcherProgress;
    private boolean composeFabShown = true;

    private void initializeFab() {
        composeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MessageComposeActivity.class));
            }
        });
        fabVisibilityChangeThreshold = (int) Utility.dpToPx(COMPOSE_FAB_VISIBILITY_CHANGE_THRESHOLD_IN_DP, getResources());

        messagesRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Math.abs(fabVisibilitySwitcherProgress) <= fabVisibilityChangeThreshold) {
                    fabVisibilitySwitcherProgress = Utility.clamp(fabVisibilitySwitcherProgress + dy, -fabVisibilityChangeThreshold, fabVisibilityChangeThreshold);
                }

                if (fabVisibilitySwitcherProgress >= fabVisibilityChangeThreshold) {
                    if (composeFabShown) {
                        composeFab.hide();
                        composeFabShown = false;
                    }
                } else if (fabVisibilitySwitcherProgress <= -fabVisibilityChangeThreshold) {
                    if (!composeFabShown) {
                        composeFab.show();
                        composeFabShown = true;
                    }
                }
            }
        });
    }


    public boolean isInboxSelected() {
        return currentFolder == MessagesList.Folder.INBOX;
    }

    private void setMessagesToAdapter(MessagesList messagesList) {
        if (messagesAdapter == null) {
            messagesAdapter = new MessagesAdapter(getActivity(), messagesList == null ? null : messagesList.getMessages(), this);
            messagesAdapter.setHasStableIds(true);
            messagesRecycler.setAdapter(messagesAdapter);
        } else
            messagesAdapter.setMessages(messagesList == null ? null : messagesList.getMessages());
        checkEmptiness(messagesList);
    }

    public void toggleFolder() {
        if (currentFolder == MessagesList.Folder.INBOX)
            currentFolder = MessagesList.Folder.SENT;
        else
            currentFolder = MessagesList.Folder.INBOX;
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(currentFolder == MessagesList.Folder.INBOX ? R.string.inbox : R.string.sent));
        viewModel.setFolder(currentFolder);
    }

    @Override
    public void onRefresh() {
        viewModel.updateMessages();
    }

    private static final int OPEN_MESSAGE = 222;
    private ShortMessage openedMessage;

    @Override
    public void onMessageClick(ShortMessage message, String messageId, boolean inbox) {
        openedMessage = message;
        Intent messageViewIntent = new Intent(getActivity(), MessageViewActivity.class);
        messageViewIntent.putExtra("messageId", messageId);
        messageViewIntent.putExtra("inbox", inbox);
        startActivityForResult(messageViewIntent, OPEN_MESSAGE);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            updateActionBarTitle();
    }

    private void checkEmptiness(MessagesList list) {
        if (list.getMessages().size() == 0)
            emptyMessages.setVisibility(View.VISIBLE);
        else
            emptyMessages.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("openedMessage", openedMessage);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_MESSAGE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (messagesAdapter.markAsRead(openedMessage))
                    viewModel.saveMessages();
            }

        }
    }

    @Override
    public void onAction(Action action) {
        switch (action) {
            case STUDENT_SWITCHED:
                break;
            case UPDATE_REQUESTED:
                viewModel.updateMessages();
                break;
        }
    }

    private void updateActionBarTitle() {
        if (!isHidden()) {
            AnalyticsHelper.viewSection(FirebaseConstants.SECTION_MESSAGES, FirebaseAnalytics.getInstance(getActivity()));
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString((currentFolder == null || currentFolder == MessagesList.Folder.INBOX) ? R.string.inbox : R.string.sent));
        }
    }
}
