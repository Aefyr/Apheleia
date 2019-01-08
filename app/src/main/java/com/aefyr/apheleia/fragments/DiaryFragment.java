package com.aefyr.apheleia.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.util.Log;
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
import com.aefyr.apheleia.helpers.AnalyticsHelper;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.DiaryHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.SerializerHelperWithTimeAndStudentKeysBase;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.minor.WeekDay;
import com.android.volley.Request;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class DiaryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, DiaryRecyclerAdapter.OnLinkOpenRequestListener, ActionListener {

    private boolean firstLoad = true;
    private Request currentRequest;
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
    private boolean quickDayPickerEnabled;

    private DiaryEntry diary;

    private Context c;

    public DiaryFragment() {
        // Required empty public constructor
    }


    private int selectedWeek;
    private String[] weeks;
    private String[] weekNames;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);
        c = inflater.getContext();
        updateActionBarTitle();

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        Utility.colorRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        emptyDiary = view.findViewById(R.id.emptyDiary);

        diaryRecycler = (RecyclerView) view.findViewById(R.id.diaryRecycler);
        diaryRecycler.setLayoutManager(Utility.displayWidthDp(getResources()) >= 720 ? new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL) : new PreloadLayoutManager(getActivity(), 7));
        diaryRecycler.setItemViewCacheSize(7);

        apiClient = EljurApiClient.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();
        diaryHelper = DiaryHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        periodsHelper = PeriodsHelper.getInstance(getActivity());
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

    private AlertDialog weeksPickerDialog;

    @Override
    public void onRefresh() {
        refreshDiary();
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

                Chief.makeASnack(getView(), getString(R.string.offline_mode));
            } else {
                antiScroll();
                Chief.makeAnAlert(c, getString(R.string.error_week_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        currentRequest = apiClient.getDiary(persona, currentStudent, days, true, new EljurApiClient.JournalismListener<DiaryEntry>() {
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
                if (tokenIsWrong) {
                    LoginActivity.tokenExpired(getActivity());
                    return;
                }
                if (!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getView(), String.format(getString(R.string.fetch_network_error), getString(R.string.diary)));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(JournalismException e) {
                if (!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getView(), getString(R.string.error_api));
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void antiScroll() {
        weeksPickerDialog.getListView().setItemChecked(selectedWeek, true);
        weeksPickerDialog.getListView().setSelection(selectedWeek);
    }

    private void setDiaryEntryToAdapter(DiaryEntry entry) {
        diary = entry;

        if (diaryRecyclerAdapter == null) {
            diaryRecyclerAdapter = new DiaryRecyclerAdapter(getActivity(), entry, DiaryFragment.this);
            diaryRecyclerAdapter.setHasStableIds(true);
            diaryRecycler.setAdapter(diaryRecyclerAdapter);
        } else
            diaryRecyclerAdapter.setDiaryEntry(entry);

        checkEmptiness(entry);
        initializeQuickScrolling(quickDayPickerEnabled, entry);
    }

    private int requestedWeek;
    private boolean brokenStudent;

    private void studentSwitched(Bundle savedInstanceState) {
        cancelRequest();
        brokenStudent = false;
        setEmptinessTextShown(false);

        if (periodsHelper.getCurrentWeek() == null) {
            brokenStudent = true;
            setDiaryEntryToAdapter(null);
            weeksPickerDialog = Utility.createBrokenStudentDialog(getActivity());
            return;
        }

        weeks = periodsHelper.getWeeks().toArray(new String[]{});
        Arrays.sort(weeks);
        selectedWeek = Arrays.binarySearch(weeks, periodsHelper.getCurrentWeek());

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
                loadDiary(weeks[requestedWeek]);
            }
        }).create();

        requestedWeek = selectedWeek;

        if (savedInstanceState != null) {
            Log.d("ADF", "savedInstanceState found");
            currentStudent = savedInstanceState.getString("currentStudent");
            Serializable diary = savedInstanceState.getSerializable("diary");
            if (diary != null) {
                setDiaryEntryToAdapter((DiaryEntry) diary);
                diaryRecycler.scrollToPosition(savedInstanceState.getInt("scrollPosition", 0));
                Log.d("ADF", "Got diary from savedInstanceState");
            } else {
                currentStudent = profileHelper.getCurrentStudentId();
                firstLoad = true;
                refreshDiary();
            }
        } else {
            currentStudent = profileHelper.getCurrentStudentId();
            firstLoad = true;
            refreshDiary();
        }
    }

    private void refreshDiary() {
        if (brokenStudent) {
            refreshLayout.setRefreshing(false);
            return;
        }
        loadDiary(weeks[selectedWeek]);
    }

    public void showTimePeriodSwitcherDialog() {
        weeksPickerDialog.show();
    }


    private void initializeQuickScrolling(boolean enabled, final DiaryEntry entry) {
        if (enabled && entry != null) {

            if (firstLoad) {
                quickDayPickBar.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams refreshLayoutParams = (RelativeLayout.LayoutParams) refreshLayout.getLayoutParams();
                refreshLayoutParams.bottomMargin = (int) -Utility.dpToPx(4, getResources());
                refreshLayout.setLayoutParams(refreshLayoutParams);
            }

            quickDayPickBar.removeAllViews();

            TimeLord timeLord = TimeLord.getInstance();

            for (final WeekDay day : entry.getDays()) {
                View quickDayPickButton = LayoutInflater.from(quickDayPickBar.getContext()).inflate(R.layout.quick_day_pick_button, quickDayPickBar, false);
                Button button = (Button) quickDayPickButton.findViewById(R.id.quickDayPickButton);
                button.setText(timeLord.getQuickPickerDate(day.getDate()));
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("diary", diary);
        outState.putString("currentStudent", currentStudent);

        if (diaryRecycler.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            int[] pos = new int[((StaggeredGridLayoutManager) diaryRecycler.getLayoutManager()).getSpanCount()];
            ((StaggeredGridLayoutManager) diaryRecycler.getLayoutManager()).findFirstVisibleItemPositions(pos);
            outState.putInt("scrollPosition", pos[0]);
        } else {
            Log.d("ADF", "scrollPos=" + ((PreloadLayoutManager) diaryRecycler.getLayoutManager()).findFirstVisibleItemPosition());
            outState.putInt("scrollPosition", ((PreloadLayoutManager) diaryRecycler.getLayoutManager()).findFirstVisibleItemPosition());
        }
    }

    private String currentStudent;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            updateActionBarTitle();
            if (!currentStudent.equals(profileHelper.getCurrentStudentId()))
                studentSwitched(null);

        } else {
            cancelRequest();
            refreshLayout.setRefreshing(false);
        }
    }

    private void cancelRequest() {
        if (currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void checkEmptiness(DiaryEntry entry) {
        setEmptinessTextShown(entry == null || entry.getDays().size() == 0);
    }

    private void setEmptinessTextShown(boolean shown) {
        emptyDiary.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAction(Action action) {
        switch (action) {
            case STUDENT_SWITCHED:
                studentSwitched(null);
                break;
            case UPDATE_REQUESTED:
                refreshDiary();
                break;
            case DATE_PICK_REQUESTED:
                showTimePeriodSwitcherDialog();
                break;
        }
    }

    private void updateActionBarTitle() {
        if (!isHidden()) {
            AnalyticsHelper.viewSection(FirebaseConstants.SECTION_DIARY, FirebaseAnalytics.getInstance(getActivity()));
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.diary));
        }
    }
}
