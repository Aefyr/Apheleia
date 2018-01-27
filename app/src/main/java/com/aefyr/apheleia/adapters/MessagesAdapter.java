package com.aefyr.apheleia.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.ShortMessage;

import java.util.ArrayList;

/**
 * Created by Aefyr on 16.08.2017.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private ArrayList<ShortMessage> messages;
    private LayoutInflater inflater;
    private TimeLord timeLord;

    private String MULTIPLE_RECEIVERS;
    private int COLOR_READ;
    private int COLOR_UNREAD;
    private int COLOR_PRIMARY;

    private OnMessageClickListener listener;

    public MessagesAdapter(Context c, ArrayList<ShortMessage> messages, OnMessageClickListener listener) {
        this.messages = messages;
        this.listener = listener;
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        timeLord = TimeLord.getInstance();

        MULTIPLE_RECEIVERS = c.getString(R.string.multiple_receivers);
        COLOR_READ = c.getResources().getColor(R.color.colorMessageRead);
        COLOR_UNREAD = c.getResources().getColor(R.color.colorMessageUnread);
        COLOR_PRIMARY = c.getResources().getColor(R.color.colorPrimary);
    }

    public void setMessages(ArrayList<ShortMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public boolean markAsRead(ShortMessage message) {
        if (message.isUnread()) {
            message.read();
            notifyItemChanged(messages.indexOf(message));
            return true;
        } else
            return false;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessageViewHolder(inflater.inflate(R.layout.message, null));
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        ShortMessage message = messages.get(position);

        holder.date.setText(timeLord.getMessageDate(message.getDate()));
        holder.subject.setText(message.getSubject());
        holder.preview.setText(message.getText());
        holder.hasFilesIcon.setVisibility(message.hasFiles() ? View.VISIBLE : View.GONE);

        if (message.getFolder() == MessagesList.Folder.INBOX) {
            holder.name.setText(message.getSender().getCompositeName(true, false, true));
        } else {
            if (message.getReceivers().size() == 1)
                holder.name.setText(message.getReceivers().get(0).getCompositeName(true, false, true));
            else
                holder.name.setText(String.format(MULTIPLE_RECEIVERS, message.getReceivers().size()));
        }

        if (message.isUnread()) {
            holder.name.setTypeface(Typeface.DEFAULT_BOLD);
            holder.subject.setTypeface(Typeface.DEFAULT_BOLD);
            holder.subject.setTextColor(COLOR_UNREAD);
            holder.date.setTypeface(Typeface.DEFAULT_BOLD);
            holder.date.setTextColor(COLOR_PRIMARY);
        } else {
            holder.name.setTypeface(Typeface.DEFAULT);
            holder.subject.setTypeface(Typeface.DEFAULT);
            holder.subject.setTextColor(COLOR_READ);
            holder.date.setTypeface(Typeface.DEFAULT);
            holder.date.setTextColor(COLOR_READ);
        }
    }

    @Override
    public int getItemCount() {
        return messages==null?0:messages.size();
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getDate();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView subject;
        private TextView preview;
        private TextView date;
        private View hasFilesIcon;

        public MessageViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.messageName);
            subject = (TextView) itemView.findViewById(R.id.messageSubject);
            preview = (TextView) itemView.findViewById(R.id.messagePreview);
            date = (TextView) itemView.findViewById(R.id.messageDate);
            hasFilesIcon = itemView.findViewById(R.id.messageAttachmentsIcon);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int index = getAdapterPosition();
                    if (index < messages.size() && index >= 0)
                        listener.onMessageClick(messages.get(index), messages.get(index).getId(), messages.get(index).getFolder() == MessagesList.Folder.INBOX);
                }
            });
        }
    }

    public interface OnMessageClickListener {
        void onMessageClick(ShortMessage message, String messageId, boolean inbox);
    }
}
