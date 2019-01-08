package com.aefyr.apheleia.adapters;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.journalism.objects.major.MessageReceiversInfo;
import com.aefyr.journalism.objects.minor.MessageReceiver;
import com.aefyr.journalism.objects.minor.MessageReceiversGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Aefyr on 19.08.2017.
 */

public class MessageReceiversAdapter extends RecyclerView.Adapter<MessageReceiversAdapter.MessageReceiversGroupViewHolder> {
    private MessageReceiversInfo receiversInfo;
    private boolean[] innerRecyclerVisibilities;

    private HashSet<String> checkedReceivers;

    private ArrayList<MessageReceiversGroupAdapter> adapters;


    protected interface OnCheckEventListener {
        void onPersonChecked(MessageReceiversGroupAdapter adapter, MessageReceiver receiver);

        void onMultiCheck(MessageReceiversGroupAdapter adapter, boolean check);

        void onPersonForceChecked(String id);
    }

    private OnCheckEventListener listener;

    public MessageReceiversAdapter(MessageReceiversInfo receiversInfo) {
        this.receiversInfo = receiversInfo;
        innerRecyclerVisibilities = new boolean[receiversInfo == null ? 0 : receiversInfo.getGroups().size()];
        Arrays.fill(innerRecyclerVisibilities, false);


        checkedReceivers = new HashSet<>();
        adapters = new ArrayList<>();

        listener = new OnCheckEventListener() {
            @Override
            public void onPersonChecked(MessageReceiversGroupAdapter adapter, MessageReceiver receiver) {
                for (MessageReceiversGroupAdapter a : adapters) {
                    if (a == adapter)
                        continue;
                    a.notifyPersonChecked(receiver);
                }
            }

            @Override
            public void onMultiCheck(MessageReceiversGroupAdapter adapter, boolean check) {

                for (MessageReceiver receiver : adapter.getGroup().getPeople()) {
                    if (check)
                        checkedReceivers.add(receiver.getId());
                    else
                        checkedReceivers.remove(receiver.getId());
                    listener.onPersonChecked(adapter, receiver);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onPersonForceChecked(String id) {
                for (MessageReceiversGroupAdapter a : adapters) {
                    a.notifyPersonForceChecked(id);
                }
            }
        };
    }

    public void setReceiversInfo(MessageReceiversInfo receiversInfo) {
        this.receiversInfo = receiversInfo;
        notifyDataSetChanged();
    }

    public void checkReceiver(String id) {
        checkedReceivers.add(id);
        listener.onPersonForceChecked(id);
    }

    public HashSet<String> getCheckedReceiversIds() {
        return checkedReceivers;
    }

    @Override
    public MessageReceiversGroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessageReceiversGroupViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_receivers_group, null));
    }

    @Override
    public void onBindViewHolder(MessageReceiversGroupViewHolder holder, int position) {
        MessageReceiversGroup group = receiversInfo.getGroups().get(position);
        holder.name.setText(group.getName());
        holder.adapter.setGroup(group);

        if (innerRecyclerVisibilities[position])
            holder.people.setVisibility(View.VISIBLE);
        else
            holder.people.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        return receiversInfo == null ? 0 : receiversInfo.getGroups().size();
    }

    @Override
    public long getItemId(int position) {
        return receiversInfo.getGroups().get(position).getName().hashCode();
    }

    class MessageReceiversGroupViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private RecyclerView people;
        private MessageReceiversGroupAdapter adapter;
        private ImageView arrow;

        MessageReceiversGroupViewHolder(final View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.receiversGroupName);
            arrow = (ImageView) itemView.findViewById(R.id.receiversGroupArrow);

            people = (RecyclerView) itemView.findViewById(R.id.receiversGroupRecycler);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(itemView.getContext());
            linearLayoutManager.setInitialPrefetchItemCount(16);
            people.setLayoutManager(linearLayoutManager);

            adapter = new MessageReceiversGroupAdapter(null, checkedReceivers, listener);
            adapter.setHasStableIds(true);
            adapters.add(adapter);
            people.setAdapter(adapter);


            (itemView.findViewById(R.id.receiversGroupOptions)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.receivers_group_options);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_select_all:
                                    listener.onMultiCheck(adapter, true);
                                    return true;
                                case R.id.action_deselect_all:
                                    listener.onMultiCheck(adapter, false);
                                    return true;
                            }
                            return false;
                        }
                    });

                    popupMenu.show();
                }
            });

            itemView.findViewById(R.id.layout).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    innerRecyclerVisibilities[getAdapterPosition()] = !innerRecyclerVisibilities[getAdapterPosition()];
                    if (innerRecyclerVisibilities[getAdapterPosition()]) {
                        people.setVisibility(View.VISIBLE);
                    } else {
                        people.setVisibility(View.GONE);
                    }
                    ObjectAnimator arrowAnim = ObjectAnimator.ofFloat(arrow, View.SCALE_Y, innerRecyclerVisibilities[getAdapterPosition()] ? -1 : 1);
                    arrowAnim.setDuration(125);
                    arrowAnim.start();
                }
            });
        }
    }

    public void writeStateToBundle(Bundle bundle) {
        bundle.putSerializable("MRA_checkedReceivers", checkedReceivers);
        bundle.putSerializable("MRA_receiversData", receiversInfo);
        bundle.putBooleanArray("MRA_visibleGroups", innerRecyclerVisibilities);
    }

    public void restoreStateFromBundle(Bundle bundle) {
        receiversInfo = (MessageReceiversInfo) bundle.getSerializable("MRA_receiversData");
        checkedReceivers = (HashSet<String>) bundle.getSerializable("MRA_checkedReceivers");
        innerRecyclerVisibilities = bundle.getBooleanArray("MRA_visibleGroups");

        notifyDataSetChanged();
    }

}
