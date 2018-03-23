package com.aefyr.apheleia.adapters;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.objects.minor.Attachment;
import com.aefyr.journalism.objects.minor.Hometask;
import com.aefyr.journalism.objects.minor.Homework;
import com.aefyr.journalism.objects.minor.Lesson;
import com.aefyr.journalism.objects.minor.Mark;
import com.aefyr.journalism.objects.minor.WeekDay;

/**
 * Created by Aefyr on 12.08.2017.
 */

class DiaryDayRecyclerAdapter extends RecyclerView.Adapter<DiaryDayRecyclerAdapter.LessonViewHolder> {
    private WeekDay day;
    private static TimeLord timeLord;
    private static DiaryRecyclerAdapter.OnLinkOpenRequestListener linkOpenRequestListener;

    private LayoutInflater inflater;

    private static String OVERTIME;
    private static int COLOR_NORMAL_LESSON;
    private static int COLOR_OT_LESSON;
    private static String TIME_UNKNOWN;


    DiaryDayRecyclerAdapter(WeekDay day, LayoutInflater inflater2) {
        this.day = day;
        inflater = inflater2;

        if (timeLord == null) {
            timeLord = TimeLord.getInstance();
            initializeStaticResources(inflater2.getContext().getResources());
        }
    }

    private void initializeStaticResources(Resources r) {
        COLOR_NORMAL_LESSON = r.getColor(R.color.colorAccent);
        COLOR_OT_LESSON = r.getColor(R.color.colorOvertimeLesson);
        OVERTIME = r.getString(R.string.overtime);
        TIME_UNKNOWN = r.getString(R.string.time_unknown);
    }

    void setDay(WeekDay day) {
        this.day = day;
        notifyDataSetChanged();
    }

    void setOnLinkOpenRequestListener(DiaryRecyclerAdapter.OnLinkOpenRequestListener listener) {
        linkOpenRequestListener = listener;
    }

    @Override
    public LessonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LessonViewHolder(inflater.inflate(R.layout.diary_day_lesson, null));
    }

    @Override
    public void onBindViewHolder(final LessonViewHolder holder, int position) {
        boolean overtimeLesson = position >= day.getLessons().size();
        Lesson lesson = overtimeLesson ? day.getOvertimeLessons().get(position - day.getLessons().size()) : day.getLessons().get(position);

        if (overtimeLesson) {
            holder.lessonNumber.setText(OVERTIME);
            holder.lessonNumber.setTextColor(COLOR_OT_LESSON);
        } else {
            holder.lessonNumber.setText(lesson.getNumber());
            holder.lessonNumber.setTextColor(COLOR_NORMAL_LESSON);
        }

        //Cause, apparently, sometimes there are HTML tags in lesson name for no real reason >_<
        holder.lessonName.setText(Html.fromHtml(lesson.getName()));

        if (lesson.hasTimes()) {
            holder.lessonTimes.setText(String.format("%s - %s", timeLord.getLessonTime(lesson.getStartTime()), timeLord.getLessonTime(lesson.getEndTime())));
        } else
            holder.lessonTimes.setText(TIME_UNKNOWN);

        if (lesson.hasHomework()) {
            Homework homework = lesson.getHomework();

            if (homework.hasTasks()) {
                StringBuilder homeworkBuilder = new StringBuilder();

                int t = 0;
                for (Hometask task : homework.getTasks()) {
                    homeworkBuilder.append("● ");
                    homeworkBuilder.append(task.getTask());
                    if (task.isPersonal()) {
                        homeworkBuilder.append(" ");
                        homeworkBuilder.append(holder.itemView.getContext().getString(R.string.personal));
                    }
                    if (t++ < lesson.getHomework().getTasks().size() - 1)
                        homeworkBuilder.append("\n");
                }

                holder.lessonHomework.setText(homeworkBuilder.toString());
            } else {
                holder.lessonHomework.setText(holder.itemView.getContext().getString(R.string.no_homework));
            }

            if (homework.hasAttachments()) {
                //Show and populate attachments container !!
                holder.attachmentsContainer.removeAllViews();
                for (final Attachment attachment : homework.getAttachments()) {
                    View view = inflater.inflate(R.layout.attachment, null);

                    Button button = (Button) view.findViewById(R.id.attachmentButton);
                    button.setText(attachment.getName());

                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openLink(attachment.getUri());
                        }
                    };
                    button.setOnClickListener(listener);
                    view.findViewById(R.id.attachmentImageButton).setOnClickListener(listener);

                    holder.attachmentsContainer.addView(view);
                }
                holder.attachmentsContainer.setVisibility(View.VISIBLE);

            } else {
                holder.attachmentsContainer.setVisibility(View.GONE);
                holder.attachmentsContainer.removeAllViews();
            }

        } else {
            holder.lessonHomework.setText(holder.itemView.getContext().getString(R.string.no_homework));
            //Hide attachments container !!
        }

        if (lesson.hasMarks()) {
            holder.marksContainer.removeAllViews();

            for (final Mark mark : lesson.getMarks()) {
                View markView = inflater.inflate(R.layout.diary_day_lesson_mark, null);
                final Button markButton = (Button) markView.findViewById(R.id.markButton);
                markButton.setText(mark.getValue());

                if (mark.hasComment()) {
                    markButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(!((Activity)inflater.getContext()).isFinishing())
                                Chief.makeAnAlert(inflater.getContext(), mark.getComment());
                        }
                    });
                } else {
                    markButton.setBackgroundColor(Color.TRANSPARENT);
                }

                if(mark.hasWeight()) {
                    TextView markWeight = markView.findViewById(R.id.markWeight);
                    markWeight.setVisibility(View.VISIBLE);
                    markWeight.setText(String.format("x%s", mark.getWeight()));
                }

                holder.marksContainer.addView(markView);
            }

            holder.marksContainer.setVisibility(View.VISIBLE);
        } else {
            holder.marksContainer.setVisibility(View.GONE);
            holder.marksContainer.removeAllViews();

        }
    }


    @Override
    public int getItemCount() {
        if (day == null)
            return 0;
        if (day.hasOvertimeLessons())
            return day.getLessons().size() + day.getOvertimeLessons().size();
        return day.getLessons().size();
    }

    @Override
    public long getItemId(int position) {
        boolean overtimeLesson = position >= day.getLessons().size();
        Lesson lesson = overtimeLesson ? day.getOvertimeLessons().get(position - day.getLessons().size()) : day.getLessons().get(position);


        if (lesson.hasTimes())
            return lesson.getStartTime();
        else
            return (lesson.getName() + position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return 444;
    }

    class LessonViewHolder extends RecyclerView.ViewHolder {
        private TextView lessonNumber;
        private TextView lessonName;
        private TextView lessonTimes;
        private TextView lessonHomework;

        private LinearLayout marksContainer;
        private LinearLayout attachmentsContainer;

        LessonViewHolder(View itemView) {
            super(itemView);
            lessonNumber = (TextView) itemView.findViewById(R.id.lessonNumber);
            lessonName = (TextView) itemView.findViewById(R.id.lessonTitle);
            lessonTimes = (TextView) itemView.findViewById(R.id.lessonTimes);
            lessonHomework = (TextView) itemView.findViewById(R.id.lessonInfo);
            marksContainer = (LinearLayout) itemView.findViewById(R.id.marksContainer);
            attachmentsContainer = (LinearLayout) itemView.findViewById(R.id.attachmentsContainer);
        }
    }

    private void openLink(String uri) {
        if (linkOpenRequestListener != null)
            linkOpenRequestListener.onLinkOpenRequest(uri);
    }
}
