package com.aefyr.apheleia.adapters;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.objects.minor.Lesson;
import com.aefyr.journalism.objects.minor.WeekDay;

/**
 * Created by Aefyr on 15.08.2017.
 */

class ScheduleDayRecyclerAdapter extends RecyclerView.Adapter<ScheduleDayRecyclerAdapter.LessonViewHolder> {
    private WeekDay day;
    private static TimeLord timeLord;

    private static LayoutInflater inflater;

    private static String TEACHER;
    private static String ROOM;
    private static String OVERTIME;
    private static int COLOR_NORMAL_LESSON;
    private static int COLOR_OT_LESSON;
    private static String TIME_UNKNOWN;

    ScheduleDayRecyclerAdapter(WeekDay day, LayoutInflater inflater2) {
        this.day = day;

        if (inflater == null) {
            timeLord = TimeLord.getInstance();
            inflater = inflater2;
            initializeStaticResources(inflater.getContext().getResources());
        }
    }

    private void initializeStaticResources(Resources r) {
        TEACHER = r.getString(R.string.teacher);
        ROOM = r.getString(R.string.room);
        COLOR_NORMAL_LESSON = r.getColor(R.color.colorAccent);
        COLOR_OT_LESSON = r.getColor(R.color.colorOvertimeLesson);
        OVERTIME = r.getString(R.string.overtime);
        TIME_UNKNOWN = r.getString(R.string.time_unknown);
    }

    void setDay(WeekDay day) {
        this.day = day;
        notifyDataSetChanged();
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

        holder.lessonName.setText(lesson.getName());

        if (lesson.hasTimes()) {
            holder.lessonTimes.setText(String.format("%s - %s", timeLord.getLessonTime(lesson.getStartTime()), timeLord.getLessonTime(lesson.getEndTime())));
        } else
            holder.lessonTimes.setText(TIME_UNKNOWN);

        holder.lessonInfo.setText(String.format("● %s: %s\n● %s: %s", TEACHER, lesson.getTeacherName(), ROOM, lesson.getRoom()));
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
        private TextView lessonInfo;

        LessonViewHolder(View itemView) {
            super(itemView);
            lessonNumber = (TextView) itemView.findViewById(R.id.lessonNumber);
            lessonName = (TextView) itemView.findViewById(R.id.lessonTitle);
            lessonTimes = (TextView) itemView.findViewById(R.id.lessonTimes);
            lessonInfo = (TextView) itemView.findViewById(R.id.lessonInfo);
        }
    }
}
