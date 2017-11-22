package com.aefyr.apheleia.adapters;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
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
    private static LayoutInflater inflater;
    private static TimeLord timeLord;

    MarksGridSubjectRecyclerAdapter(SubjectInGrid subject, LayoutInflater inflater2) {
        this.subject = subject;

        if (inflater == null) {
            inflater = inflater2;
            timeLord = TimeLord.getInstance();
        }
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
                    Chief.makeAnAlert(view.getContext(), mark.getComment());
                }
            });
            holder.mark.setBackgroundResource(R.drawable.mark_circle);
        } else {
            holder.mark.setOnClickListener(null);
            holder.mark.setBackgroundColor(Color.TRANSPARENT);
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

        MarksGridSubjectViewHolder(View itemView) {
            super(itemView);
            date = (TextView) itemView.findViewById(R.id.gridMarkDate);
            mark = (Button) itemView.findViewById(R.id.gridMarkButton);
        }
    }
}
