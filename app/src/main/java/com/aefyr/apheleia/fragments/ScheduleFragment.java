package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.Utility;
import com.aefyr.apheleia.adapters.ScheduleRecyclerAdapter;
import com.aefyr.apheleia.custom.PreloadLayoutManager;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.ScheduleHelper;
import com.aefyr.apheleia.helpers.SerializerHelperWithTimeAndStudentKeysBase;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.Schedule;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.android.volley.toolbox.StringRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private boolean firstLoad = true;
    private StringRequest currentRequest;

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


    public ScheduleFragment() {
    }


    private int selectedWeek;
    private String[] weeks;
    private String[] weekNames;
    private AlertDialog weeksPickerDialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.schedule));

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(Color.RED);

        scheduleRecycler = (RecyclerView) view.findViewById(R.id.diaryRecycler);
        scheduleRecycler.setLayoutManager(new PreloadLayoutManager(getActivity(), 7));
        scheduleRecycler.setItemViewCacheSize(7);

        emptySchedule = view.findViewById(R.id.emptyDiary);
        ((TextView)emptySchedule).setText(getString(R.string.no_schedule));

        apiClient = EljurApiClient.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();
        periodsHelper = PeriodsHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        scheduleHelper = ScheduleHelper.getInstance(getActivity());
        connectionHelper =ConnectionHelper.getInstance(getActivity());

        quickDayPickBar = (LinearLayout) view.findViewById(R.id.quickDayPickBar);

        studentSwitched();

        return view;
    }

    @Override
    public void onRefresh() {
        loadSchedule(weeks[selectedWeek]);
    }

    private void setScheduleToAdapter(Schedule schedule){
        if(scheduleRecyclerAdapter == null){
            scheduleRecyclerAdapter = new ScheduleRecyclerAdapter(getActivity(), schedule);
            scheduleRecyclerAdapter.setHasStableIds(true);
            scheduleRecycler.setAdapter(scheduleRecyclerAdapter);
        }else {
            scheduleRecyclerAdapter.setSchedule(schedule);
        }

        checkEmptiness(schedule);
        initializeQuickScrolling(true, schedule);
    }

    private boolean loadedFromMemory = false;
    private void loadSchedule(final String days){
        loadedFromMemory = false;
        refreshLayout.setRefreshing(true);

        if(firstLoad||requestedWeek!=selectedWeek||!connectionHelper.hasNetworkConnection()) {
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

        if(loadedFromMemory){
            selectedWeek = requestedWeek;
            periodsHelper.setCurrentWeek(days);
        }

        if(!connectionHelper.hasNetworkConnection()){
            if(loadedFromMemory) {

                View v = getView();
                if(v==null)
                    v = getActivity().getWindow().getDecorView();

                Chief.makeASnack(v, getString(R.string.offline_mode));
            }else {
                antiScroll();
                Chief.makeAnAlert(getActivity(), getString(R.string.error_week_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        currentRequest = apiClient.getSchedule(persona, profileHelper.getCurrentStudentId(), days, true, new EljurApiClient.JournalismListener<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                setScheduleToAdapter(result);

                scheduleHelper.saveScheduleAsync(result, days, new SerializerHelperWithTimeAndStudentKeysBase.ObjectSaveListener() {
                    @Override
                    public void onSaveCompleted(boolean successful) {
                        if(successful)
                            periodsHelper.setCurrentScheduleWeek(days);
                    }
                });

                selectedWeek = requestedWeek;

                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError() {
                if(!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getActivity().getCurrentFocus(), String.format(getString(R.string.fetch_network_error), getString(R.string.schedule)));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(String message, String json) {
                if(!loadedFromMemory)
                    antiScroll();
                Chief.makeReportApiErrorDialog(getActivity(), getString(R.string.schedule), message, json, true);
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void antiScroll(){
        weeksPickerDialog.getListView().setItemChecked(selectedWeek, true);
        weeksPickerDialog.getListView().setSelection(selectedWeek);
    }


    private int requestedWeek;
    public void studentSwitched(){
        cancelRequest();
        firstLoad = true;

        weeks = periodsHelper.getWeeks().toArray(new String[] {});
        Arrays.sort(weeks);
        selectedWeek = Arrays.binarySearch(weeks, periodsHelper.getCurrentScheduleWeek());
        weekNames = new String[weeks.length];

        SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        SimpleDateFormat target = new SimpleDateFormat("dd MMMM", Locale.getDefault());
        int i = 0;
        String[] dates;
        for(String week: weeks){
            dates = week.split("-");
            try {
                weekNames[i++] = target.format(parser.parse(dates[0]))+ " - "+ target.format(parser.parse(dates[1]));
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
                loadSchedule(weeks[requestedWeek]);
            }
        }).create();

        requestedWeek = selectedWeek;
        loadSchedule(weeks[selectedWeek]);
    }

    public void showTimePeriodSwitcherDialog(){
        weeksPickerDialog.show();
    }


    private void initializeQuickScrolling(boolean enabled, final Schedule schedule){
        if(enabled){

            if(firstLoad){
                quickDayPickBar.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams refreshLayoutParams = (RelativeLayout.LayoutParams) refreshLayout.getLayoutParams();
                refreshLayoutParams.bottomMargin = (int) -Utility.dpToPx(4, getResources());
                refreshLayout.setLayoutParams(refreshLayoutParams);
            }

            quickDayPickBar.removeAllViews();

            for(final WeekDay day: schedule.getDays()){
                View quickDayPickButton = LayoutInflater.from(quickDayPickBar.getContext()).inflate(R.layout.quick_day_pick_button, null);
                Button button = (Button) quickDayPickButton.findViewById(R.id.quickDayPickButton);
                button.setText(String.valueOf(TimeLord.getInstance().getDayTitle(day.getDate()).charAt(0)));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        scheduleRecycler.scrollToPosition(schedule.getDays().indexOf(day));
                    }
                });
                quickDayPickBar.addView(quickDayPickButton);
            }

        }else {
            quickDayPickBar.removeAllViews();
            quickDayPickBar.setVisibility(View.GONE);
        }
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

    private void checkEmptiness(Schedule schedule){
        if(schedule.getDays().size()==0)
            emptySchedule.setVisibility(View.VISIBLE);
        else
            emptySchedule.setVisibility(View.GONE);
    }
}