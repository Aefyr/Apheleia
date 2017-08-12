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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.aefyr.apheleia.Helper;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.DiaryRecyclerAdapter;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.DiaryEntry;
import com.aefyr.journalism.objects.minor.WeekDay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DiaryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{


    private SwipeRefreshLayout refreshLayout;
    private RecyclerView diaryRecycler;
    private DiaryRecyclerAdapter diaryRecyclerAdapter;
    private EljurApiClient apiClient;
    private Helper helper;
    private LinearLayout quickDayPickBar;

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

        quickDayPickBar = (LinearLayout) view.findViewById(R.id.quickDayPickBar);

        try {
            FileInputStream stream = new FileInputStream(new File(getActivity().getFilesDir(), "diary.txt"));
            ObjectInputStream objectInputStream = new ObjectInputStream(stream);
            DiaryEntry entry = (DiaryEntry) objectInputStream.readObject();

            diaryRecyclerAdapter = new DiaryRecyclerAdapter(entry);
            diaryRecycler.setAdapter(diaryRecyclerAdapter);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            loadDiary("20170515-20170519");
        }

        //loadDiary("20170515-20170519");

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

                initializeQuickScrolling(true, result);
                refreshLayout.setRefreshing(false);
                testSer(result);
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

    private void testSer(DiaryEntry entry){
        try{
            FileOutputStream fos = new FileOutputStream(new File(getActivity().getFilesDir(), "diary.txt"), false);
            ObjectOutputStream stream = new ObjectOutputStream(fos);
            stream.writeObject(entry);
            fos.close();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                        diaryRecycler.smoothScrollToPosition(entry.getDays().indexOf(day));
                    }
                });
                quickDayPickBar.addView(quickDayPickButton);
            }
        }else {
            quickDayPickBar.removeAllViews();
            quickDayPickBar.setVisibility(View.GONE);
        }
    }
}
