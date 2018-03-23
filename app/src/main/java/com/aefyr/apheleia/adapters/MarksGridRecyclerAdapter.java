package com.aefyr.apheleia.adapters;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.objects.major.MarksGrid;
import com.aefyr.journalism.objects.minor.SubjectInGrid;

/**
 * Created by Aefyr on 14.08.2017.
 */

public class MarksGridRecyclerAdapter extends RecyclerView.Adapter<MarksGridRecyclerAdapter.MarksGridViewHolder> {

    private MarksGrid grid;
    private LayoutInflater inflater;
    private static RecyclerView.RecycledViewPool marksPool;

    public MarksGridRecyclerAdapter(Context c, MarksGrid grid) {
        this.grid = grid;
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (marksPool == null) {
            marksPool = new RecyclerView.RecycledViewPool();
            marksPool.setMaxRecycledViews(1337, 322);
        }
    }

    public void setGrid(MarksGrid grid) {
        this.grid = grid;
        notifyDataSetChanged();
    }

    @Override
    public MarksGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MarksGridViewHolder(inflater.inflate(R.layout.marks_grid_subject, null));
    }

    @Override
    public void onBindViewHolder(MarksGridViewHolder holder, int position) {
        SubjectInGrid subject = grid.getLessons().get(position);

        holder.subjectName.setText(subject.getName());
        holder.subjectAverage.setText(subject.getAverageMark());

        holder.subjectRecyclerAdapter.setSubject(subject);

    }

    @Override
    public long getItemId(int position) {
        return (grid.getLessons().get(position).getName() + grid.getLessons().get(position).getAverageMark()).hashCode();
    }

    @Override
    public int getItemCount() {
        return grid==null?0:grid.getLessons().size();
    }

    @Override
    public int getItemViewType(int position) {
        return 322;
    }

    public class MarksGridViewHolder extends RecyclerView.ViewHolder {
        private MarksGridSubjectRecyclerAdapter subjectRecyclerAdapter;
        private TextView subjectName;
        private TextView subjectAverage;

        public MarksGridViewHolder(View itemView) {
            super(itemView);
            subjectName = (TextView) itemView.findViewById(R.id.gridSubjectName);
            subjectAverage = (TextView) itemView.findViewById(R.id.gridSubjectAverageValue);

            RecyclerView subjectRecycler = (RecyclerView) itemView.findViewById(R.id.gridSubjectRecycler);
            subjectRecycler.setRecycledViewPool(marksPool);

            subjectRecyclerAdapter = new MarksGridSubjectRecyclerAdapter(null, inflater);
            subjectRecyclerAdapter.setHasStableIds(true);

            int itemsCount = (int) (Utility.displayWidthPx(itemView.getResources()) / Utility.dpToPx(64, itemView.getResources()));
            if ((Utility.displayWidthDp(itemView.getResources()) >= 720))
                itemsCount /= 3;

            GridLayoutManager m = new GridLayoutManager(itemView.getContext(), itemsCount);
            m.setInitialPrefetchItemCount(128);
            subjectRecycler.setLayoutManager(m);
            subjectRecycler.setAdapter(subjectRecyclerAdapter);

        }
    }
}
