package com.aefyr.apheleia.fragments;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.DiaryRecyclerAdapter;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.minor.WeekDay;

public class DiaryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{


    private SwipeRefreshLayout refreshLayout;
    private RecyclerView diaryRecycler;
    private DiaryRecyclerAdapter diaryRecyclerAdapter;
    private EljurApiClient apiClient;
    private Helper helper;

    public DiaryFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(this);

        diaryRecycler = (RecyclerView) view.findViewById(R.id.diaryRecycler);
        diaryRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        apiClient = EljurApiClient.getInstance(getActivity());
        helper = Helper.getInstance(getActivity());

        loadDiary("20170515-20170519");

        daysEnterDialog = new AlertDialog.Builder(getActivity()).setView(R.layout.temp_edittext_layout).setTitle("Enter Days").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                loadDiary(((EditText)daysEnterDialog.findViewById(R.id.editText)).getText().toString());
            }
        }).create();

        return view;
    }

    private AlertDialog daysEnterDialog;
    @Override
    public void onRefresh() {
        daysEnterDialog.show();
    }

    private void loadDiary(String days){
        apiClient.getDiary(helper.getPersona(), helper.getCurrentStudentId(), days, true, new EljurApiClient.JournalismListener<DiaryEntry>() {
            @Override
            public void onSuccess(DiaryEntry result) {
                if(diaryRecyclerAdapter == null) {
                    diaryRecyclerAdapter = new DiaryRecyclerAdapter(result);
                    diaryRecycler.setAdapter(diaryRecyclerAdapter);
                }else
                    diaryRecyclerAdapter.setDiaryEntry(result);

                refreshLayout.setRefreshing(false);

                for(WeekDay day: result.getDays()){
                    System.out.println(String.format("Day: %d, isVacation=%b, Canon: %s", day.getDate(), day.isVacation(), day.getCanonicalName()));
                }
            }

            @Override
            public void onNetworkError() {
                new AlertDialog.Builder(getActivity()).setMessage("Network error").create().show();
            }

            @Override
            public void onApiError(String message) {
                new AlertDialog.Builder(getActivity()).setMessage(message).create().show();
            }
        });
    }
}
