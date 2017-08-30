package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.aefyr.apheleia.ActionListener;
import com.aefyr.apheleia.LoginActivity;
import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.DiaryRecyclerAdapter;
import com.aefyr.apheleia.custom.PreloadLayoutManager;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.DiaryHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.SerializerHelperWithTimeAndStudentKeysBase;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.android.volley.toolbox.StringRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class DiaryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, DiaryRecyclerAdapter.OnLinkOpenRequestListener, ActionListener {

    private boolean firstLoad = true;
    private StringRequest currentRequest;
    private View emptyDiary;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView diaryRecycler;
    private DiaryRecyclerAdapter diaryRecyclerAdapter;
    private EljurApiClient apiClient;

    private EljurPersona persona;
    private DiaryHelper diaryHelper;
    private ProfileHelper profileHelper;
    private PeriodsHelper periodsHelper;
    private ConnectionHelper connectionHelper;

    private LinearLayout quickDayPickBar;

    public DiaryFragment() {
        // Required empty public constructor
    }


    private int selectedWeek;
    private String[] weeks;
    private String[] weekNames;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.diary));

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        Utility.colorRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        emptyDiary = view.findViewById(R.id.emptyDiary);

        diaryRecycler = (RecyclerView) view.findViewById(R.id.diaryRecycler);
        diaryRecycler.setLayoutManager(Utility.displayWidthDp(getResources())>=720?new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL):new PreloadLayoutManager(getActivity(), 7));
        diaryRecycler.setItemViewCacheSize(7);

        apiClient = EljurApiClient.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();
        diaryHelper = DiaryHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        periodsHelper = PeriodsHelper.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());

        quickDayPickBar = (LinearLayout) view.findViewById(R.id.quickDayPickBar);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studentSwitched();
    }

    private AlertDialog weeksPickerDialog;

    @Override
    public void onRefresh() {
        loadDiary(weeks[selectedWeek]);
    }

    private boolean loadedFromMemory = false;

    private void loadDiary(final String days) {
        loadedFromMemory = false;
        refreshLayout.setRefreshing(true);

        if (firstLoad || requestedWeek != selectedWeek || !connectionHelper.hasNetworkConnection()) {
            if (diaryHelper.isEntrySaved(days)) {
                try {
                    setDiaryEntryToAdapter(diaryHelper.loadSavedEntry(days));
                    loadedFromMemory = true;
                    firstLoad = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (loadedFromMemory) {
            selectedWeek = requestedWeek;
            periodsHelper.setCurrentWeek(days);
        }

        if (!connectionHelper.hasNetworkConnection()) {
            if (loadedFromMemory) {

                View v = getView();
                if (v == null)
                    v = getActivity().getWindow().getDecorView();

                Chief.makeASnack(v, getString(R.string.offline_mode));
            } else {
                antiScroll();
                Chief.makeAnAlert(getActivity(), getString(R.string.error_week_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        currentRequest = apiClient.getDiary(persona, profileHelper.getCurrentStudentId(), days, true, new EljurApiClient.JournalismListener<DiaryEntry>() {
            @Override
            public void onSuccess(DiaryEntry result) {
                setDiaryEntryToAdapter(result);

                diaryHelper.saveEntryAsync(result, days, new SerializerHelperWithTimeAndStudentKeysBase.ObjectSaveListener() {
                    @Override
                    public void onSaveCompleted(boolean successful) {
                        if (successful)
                            periodsHelper.setCurrentWeek(days);
                    }
                });

                selectedWeek = requestedWeek;

                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                if(tokenIsWrong){
                    LoginActivity.tokenExpired(getActivity());
                    return;
                }
                if (!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getActivity().getCurrentFocus(), String.format(getString(R.string.fetch_network_error), getString(R.string.diary)));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(String message, String json) {
                if (!loadedFromMemory)
                    antiScroll();
                Chief.makeReportApiErrorDialog(getActivity(), getString(R.string.diary), message, json, true);
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void antiScroll() {
        weeksPickerDialog.getListView().setItemChecked(selectedWeek, true);
        weeksPickerDialog.getListView().setSelection(selectedWeek);
    }

    private void setDiaryEntryToAdapter(DiaryEntry entry) {
        if (diaryRecyclerAdapter == null) {
            diaryRecyclerAdapter = new DiaryRecyclerAdapter(getActivity(), entry, DiaryFragment.this);
            diaryRecyclerAdapter.setHasStableIds(true);
            diaryRecycler.setAdapter(diaryRecyclerAdapter);
        } else
            diaryRecyclerAdapter.setDiaryEntry(entry);

        checkEmptiness(entry);
        initializeQuickScrolling(false, entry);
    }

    private int requestedWeek;

    private void studentSwitched() {
        cancelRequest();
        firstLoad = true;

        weeks = periodsHelper.getWeeks().toArray(new String[]{});
        Arrays.sort(weeks);
        selectedWeek = Arrays.binarySearch(weeks, periodsHelper.getCurrentWeek());
        //I dunno why, but on emulator sometimes it gets a random week from periodHelper, so just to be safe, I added this
        if (!Utility.checkSelectedTime(selectedWeek, weeks))
            selectedWeek = weeks.length - 1;

        weekNames = new String[weeks.length];

        SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        SimpleDateFormat target = new SimpleDateFormat("dd MMMM", Locale.getDefault());
        int i = 0;
        String[] dates;
        for (String week : weeks) {
            dates = week.split("-");
            try {
                weekNames[i++] = target.format(parser.parse(dates[0])) + " - " + target.format(parser.parse(dates[1]));
            } catch (ParseException e) {
                e.printStackTrace();
                weekNames[i++] = "????? - ?????";
            }
        }

        weeksPickerDialog = new AlertDialog.Builder(getActivity()).setSingleChoiceItems(weekNames, selectedWeek, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                weeksPickerDialog.dismiss();
                requestedWeek = i;
                cancelRequest();
                loadDiary(weeks[requestedWeek]);
            }
        }).create();

        requestedWeek = selectedWeek;
        loadDiary(weeks[selectedWeek]);
    }

    public void showTimePeriodSwitcherDialog() {
        weeksPickerDialog.show();
    }


    private void initializeQuickScrolling(boolean enabled, final DiaryEntry entry) {
        if (enabled) {

            if (firstLoad) {
                quickDayPickBar.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams refreshLayoutParams = (RelativeLayout.LayoutParams) refreshLayout.getLayoutParams();
                refreshLayoutParams.bottomMargin = (int) -Utility.dpToPx(4, getResources());
                refreshLayout.setLayoutParams(refreshLayoutParams);
            }

            quickDayPickBar.removeAllViews();

            for (final WeekDay day : entry.getDays()) {
                View quickDayPickButton = LayoutInflater.from(quickDayPickBar.getContext()).inflate(R.layout.quick_day_pick_button, null);
                Button button = (Button) quickDayPickButton.findViewById(R.id.quickDayPickButton);
                button.setText(String.valueOf(TimeLord.getInstance().getDayTitle(day.getDate()).charAt(0)));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        diaryRecycler.scrollToPosition(entry.getDays().indexOf(day));
                    }
                });
                quickDayPickBar.addView(quickDayPickButton);
            }

        } else {
            quickDayPickBar.removeAllViews();
            quickDayPickBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLinkOpenRequest(String uri) {
        Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(linkIntent);
    }

    @Override
    public void onDetach() {
        cancelRequest();
        super.onDetach();
    }

    private void cancelRequest() {
        if (currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void checkEmptiness(DiaryEntry entry) {
        if (entry.getDays().size() == 0)
            emptyDiary.setVisibility(View.VISIBLE);
        else
            emptyDiary.setVisibility(View.GONE);
    }

    @Override
    public void onAction(Action action) {
        switch (action) {
            case STUDENT_SWITCHED:
                studentSwitched();
                break;
            case UPDATE_REQUESTED:
                loadDiary(weeks[selectedWeek]);
                break;
        }
    }
}
