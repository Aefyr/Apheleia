package com.aefyr.apheleia.adapters;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.objects.major.DiaryEntry;

import java.text.SimpleDateFormat;

/**
 * Created by Aefyr on 12.08.2017.
 */

public class DiaryRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private DiaryEntry entry;
    private TimeLord timeLord;
    private DiaryDayRecyclerAdapter.OnLinkOpenRequestListener linkOpenRequestListener;
    private static RecyclerView.RecycledViewPool lessonsPool;

    private LayoutInflater inflater;

    public DiaryRecyclerAdapter(Context c, DiaryEntry entry, DiaryDayRecyclerAdapter.OnLinkOpenRequestListener linkOpenRequestListener){
        setOnLinkOpenRequestListener(linkOpenRequestListener);
        this.entry = entry;
        timeLord = TimeLord.getInstance();
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(lessonsPool == null) {
            lessonsPool = new RecyclerView.RecycledViewPool();
            lessonsPool.setMaxRecycledViews(444, 256);
        }
    }

    public void setDiaryEntry(DiaryEntry entry){
        this.entry = entry;
        notifyDataSetChanged();
    }

    public void setOnLinkOpenRequestListener(DiaryDayRecyclerAdapter.OnLinkOpenRequestListener listener){
        linkOpenRequestListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType){
            case DayType.NORMAL:
                return new NormalDayHolder(inflater.inflate(R.layout.diary_day_normal, null));
            case DayType.VACATION:
                return  new VacationDayHolder(inflater.inflate(R.layout.diary_day_vacation, null));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()){
            case DayType.NORMAL:
                NormalDayHolder normalDayHolder = (NormalDayHolder) holder;
                normalDayHolder.dayTitle.setText(timeLord.getDayTitle(entry.getDays().get(position).getDate()));
                normalDayHolder.diaryDayRecyclerAdapter.setDay(entry.getDays().get(position));
                break;
            case DayType.VACATION:
                VacationDayHolder vacationDayHolder = (VacationDayHolder) holder;
                vacationDayHolder.dayTitle.setText(timeLord.getDayTitle(entry.getDays().get(position).getDate()));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return entry.getDays().size();
    }

    @Override
    public int getItemViewType(int position) {
        return entry.getDays().get(position).isVacation()?DayType.VACATION:DayType.NORMAL;
    }

    @Override
    public long getItemId(int position) {
        return entry.getDays().get(position).getDate();
    }

    private class DayType{
        private static final int NORMAL = 0;
        private static final int VACATION = 1;
    }

    class NormalDayHolder extends RecyclerView.ViewHolder{
        private TextView dayTitle;
        private DiaryDayRecyclerAdapter diaryDayRecyclerAdapter;
        public NormalDayHolder(View itemView) {
            super(itemView);
            dayTitle = (TextView) itemView.findViewById(R.id.dayName);

            RecyclerView lessonsRecycler = (RecyclerView) itemView.findViewById(R.id.dayLessonsRecycler);
            lessonsRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.VERTICAL, false));
            lessonsRecycler.setRecycledViewPool(lessonsPool);

            diaryDayRecyclerAdapter = new DiaryDayRecyclerAdapter(null, inflater);
            diaryDayRecyclerAdapter.setHasStableIds(true);
            diaryDayRecyclerAdapter.setOnLinkOpenRequestListener(linkOpenRequestListener);
            lessonsRecycler.setAdapter(diaryDayRecyclerAdapter);
        }
    }

    class VacationDayHolder extends RecyclerView.ViewHolder{
        private TextView dayTitle;
        public VacationDayHolder(View itemView) {
            super(itemView);
            dayTitle = (TextView) itemView.findViewById(R.id.dayName);
        }
    }
}
