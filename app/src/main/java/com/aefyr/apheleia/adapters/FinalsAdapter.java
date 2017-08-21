package com.aefyr.apheleia.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.journalism.objects.major.Finals;
import com.aefyr.journalism.objects.minor.FinalPeriod;
import com.aefyr.journalism.objects.minor.FinalSubject;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class FinalsAdapter extends RecyclerView.Adapter<FinalsAdapter.FinalsSubjectViewHolder>{

    private LayoutInflater inflater;

    private Finals finals;
    private String[] marks;

    public FinalsAdapter(Context c, Finals finals){
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.finals = finals;
        prepareMarks();
    }

    public void setFinals(Finals finals){
        this.finals = finals;
        prepareMarks();
        notifyDataSetChanged();
    }

    private void prepareMarks(){
        marks = new String[finals.getSubjects().size()];

        int i = 0;
        StringBuilder builder = new StringBuilder();

        for(FinalSubject subject: finals.getSubjects()){
            builder.delete(0, builder.length());
            for(FinalPeriod period: subject.getPeriods()){
                builder.append(period.getName()).append(": ").append(period.getMark()).append("\n");
            }

            marks[i++] = builder.toString().substring(0, builder.length()-1);
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

    class FinalsSubjectViewHolder extends RecyclerView.ViewHolder{
        private TextView name;
        private TextView marks;
        FinalsSubjectViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.subjectName);
            marks = (TextView) itemView.findViewById(R.id.subjectMarks);
        }
    }
}
