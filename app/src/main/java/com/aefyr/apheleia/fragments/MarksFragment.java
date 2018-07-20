package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
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
import com.android.volley.Request;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 */
public class MarksFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ActionListener {

    private boolean firstLoad = true;
    private Request currentRequest;

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

    private MarksGrid marks;

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
        studentSwitched(savedInstanceState);
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

                Chief.makeASnack(getView(), getString(R.string.offline_mode));
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
                Chief.makeASnack(getView(), String.format(getString(R.string.fetch_network_error), getString(R.string.marks)));
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

    private void refreshMarks() {
        if (brokenStudent) {
            refreshLayout.setRefreshing(false);
            return;
        }
        loadMarks(periods[selectedPeriod]);
    }

    private void antiScroll() {
        periodsPickerDialog.getListView().setItemChecked(selectedPeriod, true);
        periodsPickerDialog.getListView().setSelection(requestedPeriod);
    }

    private void setGridToAdapter(MarksGrid grid) {
        marks = grid;

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
    private boolean brokenStudent = false;

    private void studentSwitched(Bundle savedInstanceState) {
        cancelRequest();
        brokenStudent = false;
        setEmptinessTextShown(false);

        if (periodsHelper.getCurrentPeriod() == null) {
            brokenStudent = true;
            setGridToAdapter(null);
            periodsPickerDialog = Utility.createBrokenStudentDialog(getActivity());
            return;
        }

        periods = periodsHelper.getPeriods().toArray(new String[]{});
        Arrays.sort(periods);
        selectedPeriod = Arrays.binarySearch(periods, periodsHelper.getCurrentPeriod());

        if (!Utility.checkSelectedTime(selectedPeriod, periods))
            selectedPeriod = periods.length - 1;

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

        if (savedInstanceState != null) {
            Log.d("AMGF", "savedInstanceState found");
            currentStudent = savedInstanceState.getString("currentStudent");
            Serializable marks = savedInstanceState.getSerializable("marks");
            if (marks != null) {
                setGridToAdapter((MarksGrid) marks);
                marksRecycler.scrollToPosition(savedInstanceState.getInt("scrollPosition", 0));
                Log.d("AMGF", "Got marks from savedInstanceState");
            } else {
                currentStudent = profileHelper.getCurrentStudentId();
                firstLoad = true;
                refreshMarks();
            }
        } else {
            currentStudent = profileHelper.getCurrentStudentId();
            firstLoad = true;
            refreshMarks();
        }
    }

    public void showTimePeriodSwitcherDialog() {
        periodsPickerDialog.show();
    }

    @Override
    public void onRefresh() {
        refreshMarks();
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
                studentSwitched(null);
        } else {
            cancelRequest();
            refreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("marks", marks);
        outState.putString("currentStudent", currentStudent);

        if (marksRecycler.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            int[] pos = new int[((StaggeredGridLayoutManager) marksRecycler.getLayoutManager()).getSpanCount()];
            ((StaggeredGridLayoutManager) marksRecycler.getLayoutManager()).findFirstVisibleItemPositions(pos);
            outState.putInt("scrollPosition", pos[0]);
        } else {
            Log.d("AMGF", "scrollPos=" + ((LinearLayoutManager) marksRecycler.getLayoutManager()).findFirstVisibleItemPosition());
            outState.putInt("scrollPosition", ((LinearLayoutManager) marksRecycler.getLayoutManager()).findFirstVisibleItemPosition());
        }
    }

    private void cancelRequest() {
        if (currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    private void checkEmptiness(MarksGrid grid) {
        setEmptinessTextShown(grid == null || grid.getLessons().size() == 0);
    }

    private void setEmptinessTextShown(boolean shown) {
        emptyMarks.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAction(Action action) {
        switch (action) {
            case STUDENT_SWITCHED:
                studentSwitched(null);
                break;
            case UPDATE_REQUESTED:
                refreshMarks();
                break;
            case DATE_PICK_REQUESTED:
                showTimePeriodSwitcherDialog();
                break;
        }
    }

    private void updateActionBarTitle() {
        if (!isHidden()) {
            AnalyticsHelper.viewSection(FirebaseConstants.SECTION_MARKS, FirebaseAnalytics.getInstance(getActivity()));
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.marks));
        }
    }
}
