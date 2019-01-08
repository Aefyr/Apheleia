package com.aefyr.apheleia.adapters;

import android.app.Activity;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.objects.minor.GridMark;
import com.aefyr.journalism.objects.minor.SubjectInGrid;

/**
 * Created by Aefyr on 14.08.2017.
 */

class MarksGridSubjectRecyclerAdapter extends RecyclerView.Adapter<MarksGridSubjectRecyclerAdapter.MarksGridSubjectViewHolder> {

    private SubjectInGrid subject;
    private LayoutInflater inflater;
    private static TimeLord timeLord;

    MarksGridSubjectRecyclerAdapter(SubjectInGrid subject, LayoutInflater inflater2) {
        this.subject = subject;
        inflater = inflater2;

        if (timeLord == null)
            timeLord = TimeLord.getInstance();
    }

    void setSubject(SubjectInGrid subject) {
        this.subject = subject;
        notifyDataSetChanged();
    }

    @Override
    public MarksGridSubjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MarksGridSubjectViewHolder(inflater.inflate(R.layout.marks_grid_subject_mark, null));
    }

    @Override
    public void onBindViewHolder(final MarksGridSubjectViewHolder holder, int position) {
        final GridMark mark = subject.getMarks().get(position);

        holder.date.setText(timeLord.getGridMarkDate(mark.getDate()));
        holder.mark.setText(mark.getValue());

        if (mark.hasComment()) {
            holder.mark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!((Activity) inflater.getContext()).isFinishing())
                        Chief.makeAnAlert(inflater.getContext(), mark.getComment());
                }
            });
            holder.mark.setBackgroundResource(R.drawable.mark_circle);
        } else {
            holder.mark.setOnClickListener(null);
            holder.mark.setBackgroundColor(Color.TRANSPARENT); // forsenCD Clap
        }

        if (mark.hasWeight()) {
            holder.markWeight.setText(String.format("x%s", mark.getWeight()));
            holder.markWeight.setVisibility(View.VISIBLE);
        } else {
            holder.markWeight.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (subject == null)
            return 0;
        return subject.getMarks().size();
    }

    @Override
    public long getItemId(int position) {
        return (subject.getMarks().get(position).getDate() + subject.getMarks().get(position).getValue()).hashCode() + position;
    }

    @Override
    public int getItemViewType(int position) {
        return 1337;
    }

    public class MarksGridSubjectViewHolder extends RecyclerView.ViewHolder {

        private TextView date;
        private Button mark;
        private TextView markWeight;

        MarksGridSubjectViewHolder(View itemView) {
            super(itemView);
            date = (TextView) itemView.findViewById(R.id.gridMarkDate);
            mark = (Button) itemView.findViewById(R.id.gridMarkButton);
            markWeight = itemView.findViewById(R.id.markWeight);
        }
    }
}
