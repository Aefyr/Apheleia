package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.MarksGridRecyclerAdapter;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.MarksHelper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.android.volley.toolbox.StringRequest;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 */
public class MarksFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private boolean firstLoad = true;
    private StringRequest currentRequest;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView marksRecycler;
    private MarksGridRecyclerAdapter gridRecyclerAdapter;

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

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(this);
        marksRecycler = (RecyclerView) view.findViewById(R.id.marksRecycler);
        marksRecycler.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        marksRecycler.setItemViewCacheSize(24);

        apiClient = EljurApiClient.getInstance(getActivity());
        periodsHelper = PeriodsHelper.getInstance(getActivity());
        profileHelper = ProfileHelper.getInstance(getActivity());
        marksHelper = MarksHelper.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();

        studentSwitched();

        return view;
    }

    private boolean loadedFromMemory = false;
    private void loadMarks(final String days){
        loadedFromMemory = false;
        refreshLayout.setRefreshing(true);

        if(selectedPeriod!=requestedPeriod||firstLoad||!connectionHelper.hasNetworkConnection()) {
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

        if(loadedFromMemory){
            selectedPeriod = requestedPeriod;
            periodsHelper.setCurrentPeriod(days);
        }

        if(!connectionHelper.hasNetworkConnection()){
            if(loadedFromMemory)
                Chief.makeASnack(getActivity().getCurrentFocus(), getString(R.string.offline_mode));
            else {
                antiScroll();
                Chief.makeAnAlert(getActivity(), getString(R.string.error_grid_not_saved));
            }
            refreshLayout.setRefreshing(false);
            return;
        }

        currentRequest = apiClient.getMarks(persona, profileHelper.getCurrentStudentId(), days, new EljurApiClient.JournalismListener<MarksGrid>() {
            @Override
            public void onSuccess(MarksGrid result) {
                setGridToAdapter(result);

                marksHelper.saveGridAsync(result, days, new MarksHelper.GridSaveListener() {
                    @Override
                    public void onSaveCompleted(boolean successful) {
                        if(successful)
                            periodsHelper.setCurrentPeriod(days);
                    }
                });

                selectedPeriod = requestedPeriod;

                refreshLayout.setRefreshing(false);

            }

            @Override
            public void onNetworkError() {
                if(!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getActivity().getCurrentFocus(), getString(R.string.diary_network_error));
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onApiError(String message, String json) {
                if(!loadedFromMemory)
                    antiScroll();
                Chief.makeASnack(getActivity().getCurrentFocus(), getString(R.string.api_error)+"\n"+message);
                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void antiScroll(){
        periodsPickerDialog.getListView().setItemChecked(selectedPeriod, true);
        periodsPickerDialog.getListView().setSelection(requestedPeriod);
    }

    private void setGridToAdapter(MarksGrid grid){
        if(gridRecyclerAdapter == null){
            gridRecyclerAdapter = new MarksGridRecyclerAdapter(getActivity(), grid);
            gridRecyclerAdapter.setHasStableIds(true);
            marksRecycler.setAdapter(gridRecyclerAdapter);
        }else {
            gridRecyclerAdapter.setGrid(grid);
        }
    }

    private String[] periods;
    private String[] periodsNames;
    public void studentSwitched(){
        cancelRequest();
        firstLoad = true;
        periods = periodsHelper.getPeriods().toArray(new String[] {});
        Arrays.sort(periods);
        selectedPeriod = Arrays.binarySearch(periods, periodsHelper.getCurrentPeriod());
        periodsNames = new String[periods.length];

        int i = 0;
        for(String period:periods){
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
        loadMarks(periods[selectedPeriod]);
    }

    public void showTimePeriodSwitcherDialog(){
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

    private void cancelRequest(){
        if(currentRequest != null && !currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }
}
