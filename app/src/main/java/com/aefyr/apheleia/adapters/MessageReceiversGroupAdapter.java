package com.aefyr.apheleia.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.journalism.objects.minor.MessageReceiver;
import com.aefyr.journalism.objects.minor.MessageReceiversGroup;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Created by Aefyr on 19.08.2017.
 */

public class MessageReceiversGroupAdapter extends RecyclerView.Adapter<MessageReceiversGroupAdapter.MessagePersonViewHolder>{

    private MessageReceiversGroup group;

    private HashSet<String> checkedReceivers;

    private MessageReceiversAdapter.OnCheckEventListener listener;

    public MessageReceiversGroupAdapter(MessageReceiversGroup group, HashSet<String> checkedReceivers, MessageReceiversAdapter.OnCheckEventListener listener){
        this.checkedReceivers = checkedReceivers;
        this.group = group;
        this.listener = listener;
    }

    public void setGroup(MessageReceiversGroup group){
        this.group = group;
        Collections.sort(group.getPeople(), new Comparator<MessageReceiver>() {
            @Override
            public int compare(MessageReceiver o1, MessageReceiver o2) {
                return o1.getCompositeName(true, false, false ).compareTo(o2.getCompositeName(true, false, false ));
            }
        });
        notifyDataSetChanged();
    }

    MessageReceiversGroup getGroup(){
        return group;
    }

    public void notifyPersonChecked(MessageReceiver receiver){
        int i = group.getPeople().indexOf(receiver);
        if(i!=-1)
            notifyItemChanged(i);
    }

    public void notifyPersonForceChecked(String id){
        int i = 0;
        for(MessageReceiver r :group.getPeople()){
            if(r.getId().equals(id))
                notifyItemChanged(i);
            i++;
        }
    }

    @Override
    public MessagePersonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessagePersonViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_receiver, null));
    }


    @Override
    public void onBindViewHolder(MessagePersonViewHolder holder, int position) {
        MessageReceiver receiver = group.getPeople().get(position);
        holder.binding = true;
        holder.name.setText(receiver.getCompositeName(true, true, true));
        holder.checkBox.setChecked(checkedReceivers.contains(receiver.getId()));
        if(receiver.hasInfo()){
            holder.info.setText(receiver.getInfo());
            holder.info.setVisibility(View.VISIBLE);
        }else
            holder.info.setVisibility(View.GONE);
        holder.binding = false;
    }

    @Override
    public int getItemCount() {
        return group==null?0:group.getPeople().size();
    }

    @Override
    public long getItemId(int position) {
        return Long.valueOf(group.getPeople().get(position).getId())+position+group.getPeople().size();
    }

    class MessagePersonViewHolder extends RecyclerView.ViewHolder{

        private TextView name;
        private TextView info;
        private CheckBox checkBox;
        private boolean binding = false;

        MessagePersonViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.personName);
            info = (TextView) itemView.findViewById(R.id.personInfo);

            checkBox = (CheckBox) itemView.findViewById(R.id.personCheckBox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(binding)
                        return;
                    MessageReceiver clickedPerson = getGroup().getPeople().get(getAdapterPosition());
                    if(isChecked)
                        checkedReceivers.add(clickedPerson.getId());
                    else
                        checkedReceivers.remove(clickedPerson.getId());
                    listener.onPersonChecked(MessageReceiversGroupAdapter.this, clickedPerson);
                }
            });

            itemView.findViewById(R.id.personLayout).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            });
        }
    }
}
