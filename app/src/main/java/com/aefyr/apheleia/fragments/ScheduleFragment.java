package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aefyr.apheleia.ActionListener;
import com.aefyr.apheleia.LoginActivity;
import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.ScheduleRecyclerAdapter;
import com.aefyr.apheleia.custom.PreloadLayoutManager;
import com.aefyr.apheleia.helpers.AnalyticsHelper;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.ScheduleHelper;
import com.aefyr.apheleia.helpers.SerializerHelperWithTimeAndStudentKeysBase;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.Schedule;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.android.volley.Request;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ActionListener {

    private boolean firstLoad = true;
    private Request currentRequest;

    private View emptySchedule;

    private EljurApiClient apiClient;
    private EljurPersona persona;
    private PeriodsHelper periodsHelper;
    private ProfileHelper profileHelper;
    private ScheduleHelper scheduleHelper;
    private ConnectionHelper connectionHelper;

    private RecyclerView scheduleRecycler;
    private ScheduleRecyclerAdapter scheduleRecyclerAdapter;
    private SwipeRefreshLayout refreshLayout;

    private LinearLayout quickDayPickBar;
    private boolean quickDayPickerEnabled;

    private Schedule schedule;


    public ScheduleFragment() {
    }


    private int selectedWeek;
    private String[] weeks;
    private String[] weekNames;
    private AlertDialog weeksPickerDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);
        updateActionBarTitle();

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        Utility.colorRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this);

        scheduleRecycler = (RecyclerView) view.findViewById(R.id.diaryRecycler);
        scheduleRecycler.setLayoutManager(Utility.displayWidthDp(getResources()) >= 720 ? new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL) : new PreloadLayoutManager(getActivity(), 7));
        scheduleRecycler.setItemViewCacheSize(7);

        emptySchedule = view.findViewById(R.id.emptyDiary);
        ((TextView) emptySchedule).setText(getString(R.string.no_schedule));

        apiClient = EljurApiClient.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();
        periodsHelper = PeriodsHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        scheduleHelper = ScheduleHelper.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());

        quickDayPickBar = (LinearLayout) view.findViewById(R.id.quickDayPickBar);
        quickDayPickerEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("quick_day_picker_enabled", true);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studentSwitched(savedInstanceState);
    }

    @Override
    public void onRefresh() {
        refreshSchedule();
    }

    private void setScheduleToAdapter(Schedule schedule) {
        this.schedule = schedule;

        if (scheduleRecyclerAdapter == null) {
            scheduleRecyclerAdapter = new ScheduleRecyclerAdapter(getActivity(), schedule);
            scheduleRecyclerAdapter.setHasStableIds(true);
            scheduleRecycler.setAdapter(scheduleRecyclerAdapter);
        } else {
            scheduleRecyclerAdapter.setSchedule(schedule);
        }

        checkEmptiness(schedule);
        initializeQuickScrolling(quickDayPickerEnabled, schedule);
    }

    private boolean loadedFromMemory = false;

    private void loadSchedule(final String days) {

        loadedFromMemory = false;
        refreshLayout.setRefreshing(true);

        if (firstLoad || requestedWeek != selectedWeek || !connectionHelper.hasNetworkConnection()) {
            if (scheduleHelper.isScheduleSaved(days)) {
                try {
                    setScheduleToAdapter(scheduleHelper.loadSavedSchedule(days));
                    loadedFromMemory = true;
                    firstLoad = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (loadedFromMemory) {
            selectedWeek = requestedWeek;
            periodsHelper.setCurrentScheduleWeek(days);
        }

        if (!connectionHelper.hasNetworkConnection()) {
            if (loadedFromMemory) {

                Chief.makeASnack(getView(), getString(R.string.offline_mode));
            } else {
                antiScroll();
                Chief.makeAnAlert(getActivity(), getString(R.string.error_week_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        currentRequest = apiClient.getSchedule(persona, currentStudent, days, true, new EljurApiClient.JournalismListener<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                setScheduleToAdapter(result);

                scheduleHelper.saveScheduleAsync(result, days, new SerializerHelperWithTimeAndStudentKeysBase.ObjectSaveListener() {
                    @Override
                    public void onSaveCompleted(boolean successful) {
                        if (successful)
                            periodsHelper.setCurrentScheduleWeek(days);
                    }
                });

                selectedWeek = requestedWeek;

                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                if (tokenIsWrong) {
                    LoginActivity.tokenExpired(getActivity());
                    return;
                }
                if (!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getView(), String.format(getString(R.string.fetch_network_error), getString(R.string.schedule)));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(JournalismException e) {
                if (!loadedFromMemory)
                    antiScroll();
                Crashlytics.logException(e);
                Chief.makeApiErrorAlert(getActivity(), false);
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void refreshSchedule(){
        if(brokenStudent) {
            refreshLayout.setRefreshing(false);
            return;
        }
        loadSchedule(weeks[selectedWeek]);
    }

    private void antiScroll() {
        weeksPickerDialog.getListView().setItemChecked(selectedWeek, true);
        weeksPickerDialog.getListView().setSelection(selectedWeek);
    }


    private int requestedWeek;
    private boolean brokenStudent = false;
    private void studentSwitched(Bundle savedInstanceState) {
        cancelRequest();
        brokenStudent = false;
        setEmptinessTextShown(false);

        if(periodsHelper.getCurrentScheduleWeek() == null){
            brokenStudent = true;
            setScheduleToAdapter(null);
            weeksPickerDialog = Utility.createBrokenStudentDialog(getActivity());
            return;
        }

        weeks = periodsHelper.getWeeks().toArray(new String[]{});
        Arrays.sort(weeks);
        selectedWeek = Arrays.binarySearch(weeks, periodsHelper.getCurrentScheduleWeek());

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
                Crashlytics.logException(e);
                weekNames[i++] = "????? - ?????";
            }
        }

        weeksPickerDialog = new AlertDialog.Builder(getActivity()).setSingleChoiceItems(weekNames, selectedWeek, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                weeksPickerDialog.dismiss();
                requestedWeek = i;
                cancelRequest();
                loadSchedule(weeks[requestedWeek]);
            }
        }).create();

        requestedWeek = selectedWeek;

        if(savedInstanceState!=null){
            Log.d("ASF", "savedInstanceState found");
            currentStudent = savedInstanceState.getString("currentStudent");
            Serializable schedule = savedInstanceState.getSerializable("schedule");
            if(schedule!=null) {
                setScheduleToAdapter((Schedule) schedule);
                scheduleRecycler.scrollToPosition(savedInstanceState.getInt("scrollPosition", 0));
                Log.d("ASF", "Got diary from savedInstanceState");
            }else {
                currentStudent = profileHelper.getCurrentStudentId();
                firstLoad = true;
                refreshSchedule();
            }
        }else {
            currentStudent = profileHelper.getCurrentStudentId();
            firstLoad = true;
            refreshSchedule();
        }
    }

    public void showTimePeriodSwitcherDialog() {
        weeksPickerDialog.show();
    }


    private void initializeQuickScrolling(boolean enabled, final Schedule schedule) {
        if (enabled && schedule!=null) {

            if (firstLoad) {
                quickDayPickBar.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams refreshLayoutParams = (RelativeLayout.LayoutParams) refreshLayout.getLayoutParams();
                refreshLayoutParams.bottomMargin = (int) -Utility.dpToPx(4, getResources());
                refreshLayout.setLayoutParams(refreshLayoutParams);
            }

            quickDayPickBar.removeAllViews();

            TimeLord timeLord = TimeLord.getInstance();

            for (final WeekDay day : schedule.getDays()) {
                View quickDayPickButton = LayoutInflater.from(quickDayPickBar.getContext()).inflate(R.layout.quick_day_pick_button, quickDayPickBar, false);
                Button button = (Button) quickDayPickButton.findViewById(R.id.quickDayPickButton);
                button.setText(String.valueOf(timeLord.getQuickPickerDate(day.getDate())));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        scheduleRecycler.scrollToPosition(schedule.getDays().indexOf(day));
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
    public void onDetach() {
        cancelRequest();
        super.onDetach();
    }

    private String currentStudent;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            updateActionBarTitle();
        if (!currentStudent.equals(profileHelper.getCurrentStudentId()))
            studentSwitched(null);
        else {
            cancelRequest();
            refreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("schedule", schedule);
        outState.putString("currentStudent", currentStudent);

        if(scheduleRecycler.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            int[] pos = new int[((StaggeredGridLayoutManager) scheduleRecycler.getLayoutManager()).getSpanCount()];
            ((StaggeredGridLayoutManager) scheduleRecycler.getLayoutManager()).findFirstVisibleItemPositions(pos);
            outState.putInt("scrollPosition", pos[0]);
        }else {
            Log.d("ASF", "scrollPos=" + ((PreloadLayoutManager) scheduleRecycler.getLayoutManager()).findFirstVisibleItemPosition());
            outState.putInt("scrollPosition", ((PreloadLayoutManager) scheduleRecycler.getLayoutManager()).findFirstVisibleItemPosition());
        }
    }

    private void cancelRequest() {
        if (currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void checkEmptiness(Schedule schedule) {
        setEmptinessTextShown(schedule == null || schedule.getDays().size() == 0);
    }

    private void setEmptinessTextShown(boolean shown){
        emptySchedule.setVisibility(shown?View.VISIBLE:View.GONE);
    }

    @Override
    public void onAction(Action action) {
        switch (action) {
            case STUDENT_SWITCHED:
                studentSwitched(null);
                break;
            case UPDATE_REQUESTED:
                refreshSchedule();
                break;
        }
    }

    private void updateActionBarTitle() {
        if(!isHidden()) {
            AnalyticsHelper.viewSection(FirebaseConstants.SECTION_SCHEDULE, FirebaseAnalytics.getInstance(getActivity()));
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.schedule));
        }
    }
}
