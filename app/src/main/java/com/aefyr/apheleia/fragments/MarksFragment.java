package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.ActionListener;
import com.aefyr.apheleia.LoginActivity;
import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.MarksGridRecyclerAdapter;
import com.aefyr.apheleia.helpers.AnalyticsHelper;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.MarksHelper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.SerializerHelperWithTimeAndStudentKeysBase;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 */
public class MarksFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ActionListener {

    private boolean firstLoad = true;
    private StringRequest currentRequest;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView marksRecycler;
    private MarksGridRecyclerAdapter gridRecyclerAdapter;
    private View emptyMarks;

    private EljurApiClient apiClient;
    private PeriodsHelper periodsHelper;
    private EljurPersona persona;
    private ProfileHelper profileHelper;
    private MarksHelper marksHelper;
    private ConnectionHelper connectionHelper;

    private int selectedPeriod;
    private int requestedPeriod;

    private AlertDialog periodsPickerDialog;

    public MarksFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_marks, container, false);
        updateActionBarTitle();

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        Utility.colorRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        emptyMarks = view.findViewById(R.id.emptyMarks);
        marksRecycler = (RecyclerView) view.findViewById(R.id.marksRecycler);
        marksRecycler.setLayoutManager(Utility.displayWidthDp(getResources()) >= 720 ? new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL) : new LinearLayoutManager(getActivity()));
        marksRecycler.setItemViewCacheSize(24);

        apiClient = EljurApiClient.getInstance(getActivity());
        periodsHelper = PeriodsHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        marksHelper = MarksHelper.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AnalyticsHelper.viewSection(FirebaseConstants.SECTION_MARKS, FirebaseAnalytics.getInstance(getActivity()));
        studentSwitched();
    }

    private boolean loadedFromMemory = false;

    private void loadMarks(final String days) {
        loadedFromMemory = false;
        refreshLayout.setRefreshing(true);

        if (selectedPeriod != requestedPeriod || firstLoad || !connectionHelper.hasNetworkConnection()) {
            if (marksHelper.isGridSaved(days)) {
                try {
                    setGridToAdapter(marksHelper.loadSavedGrid(days));
                    loadedFromMemory = true;
                    System.out.println("Loaded period from memory");
                    firstLoad = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (loadedFromMemory) {
            selectedPeriod = requestedPeriod;
            periodsHelper.setCurrentPeriod(days);
        }

        if (!connectionHelper.hasNetworkConnection()) {
            if (loadedFromMemory) {
                View v = getView();
                if (v == null)
                    v = getActivity().getWindow().getDecorView();

                Chief.makeASnack(v, getString(R.string.offline_mode));
            } else {
                antiScroll();
                Chief.makeAnAlert(getActivity(), getString(R.string.error_grid_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        currentRequest = apiClient.getMarks(persona, currentStudent, days, new EljurApiClient.JournalismListener<MarksGrid>() {
            @Override
            public void onSuccess(MarksGrid result) {
                setGridToAdapter(result);

                marksHelper.saveGridAsync(result, days, new SerializerHelperWithTimeAndStudentKeysBase.ObjectSaveListener() {
                    @Override
                    public void onSaveCompleted(boolean successful) {
                        if (successful)
                            periodsHelper.setCurrentPeriod(days);
                    }
                });

                selectedPeriod = requestedPeriod;

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
                Chief.makeASnack(getActivity().getCurrentFocus(), String.format(getString(R.string.fetch_network_error), getString(R.string.marks)));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(JournalismException e) {
                if (!loadedFromMemory)
                    antiScroll();
                FirebaseCrash.report(e);
                Chief.makeApiErrorAlert(getActivity(), false);
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void antiScroll() {
        periodsPickerDialog.getListView().setItemChecked(selectedPeriod, true);
        periodsPickerDialog.getListView().setSelection(requestedPeriod);
    }

    private void setGridToAdapter(MarksGrid grid) {
        if (gridRecyclerAdapter == null) {
            gridRecyclerAdapter = new MarksGridRecyclerAdapter(getActivity(), grid);
            gridRecyclerAdapter.setHasStableIds(true);
            marksRecycler.setAdapter(gridRecyclerAdapter);
        } else {
            gridRecyclerAdapter.setGrid(grid);
        }
        checkEmptiness(grid);
    }

    private String[] periods;
    private String[] periodsNames;

    private void studentSwitched() {
        cancelRequest();
        firstLoad = true;
        periods = periodsHelper.getPeriods().toArray(new String[]{});
        Arrays.sort(periods);
        selectedPeriod = Arrays.binarySearch(periods, periodsHelper.getCurrentPeriod());
        periodsNames = new String[periods.length];

        int i = 0;
        for (String period : periods) {
            periodsNames[i++] = periodsHelper.getPeriodName(period);
        }

        periodsPickerDialog = new AlertDialog.Builder(getActivity()).setSingleChoiceItems(periodsNames, selectedPeriod, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                periodsPickerDialog.dismiss();
                requestedPeriod = i;
                cancelRequest();
                loadMarks(periods[requestedPeriod]);
            }
        }).create();

        requestedPeriod = selectedPeriod;
        currentStudent = profileHelper.getCurrentStudentId();
        loadMarks(periods[selectedPeriod]);
    }

    public void showTimePeriodSwitcherDialog() {
        periodsPickerDialog.show();
    }

    @Override
    public void onRefresh() {
        loadMarks(periods[selectedPeriod]);
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
        if (!hidden) {
            updateActionBarTitle();
            if (!currentStudent.equals(profileHelper.getCurrentStudentId()))
                studentSwitched();
        } else {
            cancelRequest();
            refreshLayout.setRefreshing(false);
        }
    }

    private void cancelRequest() {
        if (currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void checkEmptiness(MarksGrid grid) {
        if (grid.getLessons().size() == 0)
            emptyMarks.setVisibility(View.VISIBLE);
        else
            emptyMarks.setVisibility(View.GONE);
    }

    @Override
    public void onAction(Action action) {
        switch (action) {
            case STUDENT_SWITCHED:
                studentSwitched();
                break;
            case UPDATE_REQUESTED:
                loadMarks(periods[selectedPeriod]);
                break;
        }
    }

    private void updateActionBarTitle() {
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.marks));
    }
}
