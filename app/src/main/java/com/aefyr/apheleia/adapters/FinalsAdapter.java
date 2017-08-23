package com.aefyr.apheleia.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.journalism.objects.major.Finals;
import com.aefyr.journalism.objects.minor.FinalPeriod;
import com.aefyr.journalism.objects.minor.FinalSubject;

import java.util.HashMap;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class FinalsAdapter extends RecyclerView.Adapter<FinalsAdapter.FinalsSubjectViewHolder> {

    private LayoutInflater inflater;

    private Finals finals;
    private SpannableStringBuilder[] marks;

    private int[] colors;
    private HashMap<String, Integer> colorMap;
    private int lastColor = 0;

    public FinalsAdapter(Context c, Finals finals) {
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.finals = finals;
        colors = c.getResources().getIntArray(R.array.periods_palette);
        colorMap = new HashMap<>();
        prepareMarks();
    }

    public void setFinals(Finals finals) {
        this.finals = finals;
        prepareMarks();
        notifyDataSetChanged();
    }

    private void prepareMarks() {
        marks = new SpannableStringBuilder[finals.getSubjects().size()];

        int i = 0;

        for (FinalSubject subject : finals.getSubjects()) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            int l = 0;
            for (FinalPeriod period : subject.getPeriods()) {
                if(!colorMap.containsKey(period.getName())){
                    colorMap.put(period.getName(), colors[lastColor++%colors.length]);
                }
                if(Build.VERSION.SDK_INT>=21)
                    builder.append((period.getName()+": "+period.getMark()+"\n"), new ForegroundColorSpan(colorMap.get(period.getName())), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                else {
                    builder.append(period.getName()).append(": ").append(period.getMark()).append("\n");
                    builder.setSpan(new ForegroundColorSpan(colorMap.get(period.getName())), l, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    l = builder.length();
                }

            }
            builder.delete(builder.length()-1, builder.length());
            marks[i++] = builder;
        }
    }

    @Override
    public FinalsSubjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FinalsSubjectViewHolder(inflater.inflate(R.layout.finals_subject, null));
    }

    @Override
    public void onBindViewHolder(FinalsSubjectViewHolder holder, int position) {
        holder.name.setText(finals.getSubjects().get(position).getName());
        holder.marks.setText(marks[position]);
    }

    @Override
    public int getItemCount() {
        return finals.getSubjects().size();
    }

    @Override
    public long getItemId(int position) {
        return finals.getSubjects().get(position).getName().hashCode();
    }

    class FinalsSubjectViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView marks;

        FinalsSubjectViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.subjectName);
            marks = (TextView) itemView.findViewById(R.id.subjectMarks);
        }
    }
}
