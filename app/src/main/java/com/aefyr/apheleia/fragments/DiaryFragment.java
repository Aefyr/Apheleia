package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.DiaryDayRecyclerAdapter;
import com.aefyr.apheleia.adapters.DiaryRecyclerAdapter;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.DiaryHelper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.minor.WeekDay;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class DiaryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, DiaryDayRecyclerAdapter.OnLinkOpenRequestListener{


    private SwipeRefreshLayout refreshLayout;
    private RecyclerView diaryRecycler;
    private DiaryRecyclerAdapter diaryRecyclerAdapter;
    private EljurApiClient apiClient;

    private Helper helper;
    private DiaryHelper diaryHelper;
    private ProfileHelper profileHelper;
    private PeriodsHelper periodsHelper;
    private ConnectionHelper connectionHelper;

    private LinearLayout quickDayPickBar;

    public DiaryFragment() {
        // Required empty public constructor
    }




    int selectedWeek;
    String[] weeks;
    String[] weekNames;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(this);

        diaryRecycler = (RecyclerView) view.findViewById(R.id.diaryRecycler);
        diaryRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        apiClient = EljurApiClient.getInstance(getActivity());
        helper = Helper.getInstance(getActivity());
        diaryHelper = DiaryHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        periodsHelper = PeriodsHelper.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());

        quickDayPickBar = (LinearLayout) view.findViewById(R.id.quickDayPickBar);

        studentSwitched();

        return view;
    }

    private AlertDialog daysEnterDialog;
    @Override
    public void onRefresh() {
        daysEnterDialog.show();
    }

    private void loadDiary(final String days){
        boolean loadedFromMemory = false;
        refreshLayout.setRefreshing(true);
        if(diaryHelper.isEntrySaved(days)){
            try {
                setDiaryEntryToAdapter(diaryHelper.loadSavedEntry(days));
                loadedFromMemory = true;

                periodsHelper.setCurrentWeek(days);
                selectedWeek = requestedWeek;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(loadedFromMemory){
            selectedWeek = requestedWeek;
            periodsHelper.setCurrentWeek(days);
        }

        if(!connectionHelper.hasNetworkConnection()){
            if(loadedFromMemory) {
                //TODO set selectedWeek selected in daysDialog
                Chief.makeASnack(getView(), getString(R.string.offline_mode));
            }else {
                Chief.makeAnAlert(getActivity(), getString(R.string.error_week_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        apiClient.getDiary(helper.getPersona(), profileHelper.getCurrentStudentId(), days, true, new EljurApiClient.JournalismListener<DiaryEntry>() {
            @Override
            public void onSuccess(DiaryEntry result) {
                setDiaryEntryToAdapter(result);

                if(diaryHelper.saveEntry(result, days))
                    periodsHelper.setCurrentWeek(days);

                selectedWeek = requestedWeek;

                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError() {
                //TODO set selectedWeek selected in daysDialog
                Chief.makeASnack(getView(), getString(R.string.network_error_tip));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(String message) {
                //TODO set selectedWeek selected in daysDialog
                Chief.makeASnack(getView(), getString(R.string.api_error)+"\n"+message);
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void setDiaryEntryToAdapter(DiaryEntry entry){
        if(diaryRecyclerAdapter == null) {
            diaryRecyclerAdapter = new DiaryRecyclerAdapter(entry, DiaryFragment.this);
            diaryRecyclerAdapter.setHasStableIds(true);
            diaryRecycler.setAdapter(diaryRecyclerAdapter);
        }else
            diaryRecyclerAdapter.setDiaryEntry(entry);

        initializeQuickScrolling(false, entry);
    }

    private int requestedWeek;
    public void studentSwitched(){
        weeks = periodsHelper.getWeeks().toArray(new String[] {});
        Arrays.sort(weeks);
        selectedWeek = Arrays.binarySearch(weeks, periodsHelper.getCurrentWeek());

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

        daysEnterDialog = new AlertDialog.Builder(getActivity()).setSingleChoiceItems(weekNames, selectedWeek, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                daysEnterDialog.dismiss();
                loadDiary(weeks[i]);
                requestedWeek = i;
            }
        }).create();

        loadDiary(weeks[selectedWeek]);
    }




    private void initializeQuickScrolling(boolean enabled, final DiaryEntry entry){
        if(enabled){
            quickDayPickBar.removeAllViews();

            for(final WeekDay day: entry.getDays()){
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
        }else {
            quickDayPickBar.removeAllViews();
            quickDayPickBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLinkOpenRequest(String uri) {
        System.out.println("LinkOpenRequest: "+uri);
        Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(linkIntent);
    }
}
