package com.aefyr.apheleia.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.ActionListener;
import com.aefyr.apheleia.helpers.AnalyticsHelper;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.apheleia.adapters.FinalsAdapter;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.ConnectionHelper;
import com.aefyr.apheleia.helpers.FinalsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.Finals;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * A simple {@link Fragment} subclass.
 */
public class FinalsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ActionListener{

    private StringRequest currentRequest;
    private boolean firstLoad = true;

    private SwipeRefreshLayout refreshLayout;
    private FinalsAdapter finalsAdapter;
    private RecyclerView finalsRecycler;
    private View emptyFinals;

    private EljurPersona persona;
    private EljurApiClient apiClient;
    private ProfileHelper profileHelper;
    private FinalsHelper finalsHelper;
    private ConnectionHelper connectionHelper;

    public FinalsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finals, container, false);
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.finals));
        AnalyticsHelper.logAppSectionViewEvent(FirebaseAnalytics.getInstance(getActivity()), FirebaseConstants.SECTION_FINALS);

        finalsRecycler = (RecyclerView) view.findViewById(R.id.finalsRecycler);
        finalsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        Utility.colorRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this);
        emptyFinals = view.findViewById(R.id.emptyFinals);

        apiClient = EljurApiClient.getInstance(getActivity());
        persona = Helper.getInstance(getActivity()).getPersona();
        profileHelper = ProfileHelper.getInstance(getActivity());
        finalsHelper = FinalsHelper.getInstance(getActivity());
        connectionHelper = ConnectionHelper.getInstance(getActivity());

        loadFinals();

        return view;
    }

    private void setFinalsToAdapter(Finals finals){
        if(finalsAdapter==null){
            finalsAdapter = new FinalsAdapter(getActivity(), finals);
            finalsAdapter.setHasStableIds(true);
            finalsRecycler.setAdapter(finalsAdapter);
        }else
            finalsAdapter.setFinals(finals);
        checkEmptiness(finals);
    }

    private boolean loadedFromMemory = false;
    private void loadFinals(){
        refreshLayout.setRefreshing(true);

        if(firstLoad){
            loadedFromMemory = false;
            try {
                setFinalsToAdapter(finalsHelper.loadFinals());
                loadedFromMemory = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(!connectionHelper.hasNetworkConnection()&&loadedFromMemory){
            View v = getView();
            if(v==null)
                v = getActivity().getWindow().getDecorView();
            Chief.makeASnack(v, getString(R.string.offline_mode));
            refreshLayout.setRefreshing(false);
            return;
        }


        currentRequest = apiClient.getFinals(persona, profileHelper.getCurrentStudentId(), new EljurApiClient.JournalismListener<Finals>() {
            @Override
            public void onSuccess(Finals result) {
                finalsHelper.saveFinalsAsync(result, null);
                setFinalsToAdapter(result);
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void onNetworkError() {
                refreshLayout.setRefreshing(false);
                Chief.makeASnack(getActivity().getCurrentFocus(), String.format(getString(R.string.fetch_network_error), getString(R.string.finals)));
            }

            @Override
            public void onApiError(String message, String json) {
                refreshLayout.setRefreshing(false);
                Chief.makeReportApiErrorDialog(getActivity(), getString(R.string.finals), message, json, true);
            }
        });
    }

    @Override
    public void onRefresh() {
        loadFinals();
    }

    public void studentSwitched(){
        firstLoad = true;
        loadFinals();
    }

    private void checkEmptiness(Finals finals){
        if(finals.getSubjects().size()==0)
            emptyFinals.setVisibility(View.VISIBLE);
        else
            emptyFinals.setVisibility(View.GONE);
    }

    private void cancel(){
        if(currentRequest!=null&&!currentRequest.hasHadResponseDelivered())
            currentRequest.cancel();
    }

    @Override
    public void onDetach() {
        cancel();
        super.onDetach();
    }

    @Override
    public void onAction(Action action) {
        switch (action){
            case STUDENT_SWITCHED:
            case UPDATE_REQUESTED:
                loadFinals();
                break;
        }
    }
}
