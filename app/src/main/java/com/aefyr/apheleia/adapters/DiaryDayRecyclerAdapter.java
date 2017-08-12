package com.aefyr.apheleia.adapters;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.objects.minor.Hometask;
import com.aefyr.journalism.objects.minor.Homework;
import com.aefyr.journalism.objects.minor.Lesson;
import com.aefyr.journalism.objects.minor.Mark;
import com.aefyr.journalism.objects.minor.WeekDay;

/**
 * Created by Aefyr on 12.08.2017.
 */

public class DiaryDayRecyclerAdapter extends RecyclerView.Adapter<DiaryDayRecyclerAdapter.LessonViewHolder>{
    private WeekDay day;
    private TimeLord timeLord;

    public DiaryDayRecyclerAdapter(WeekDay day){
        this.day = day;
        timeLord = TimeLord.getInstance();
    }

    public void setDay(WeekDay day){
        this.day = day;
        notifyDataSetChanged();
    }

    @Override
    public LessonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LessonViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.diary_day_lesson, null));
    }

    @Override
    public void onBindViewHolder(final LessonViewHolder holder, int position) {
        Lesson lesson = day.getLessons().get(position);
        holder.lessonNumber.setText(lesson.getNumber());
        holder.lessonName.setText(lesson.getName());
        if(lesson.hasTimes()) {
            holder.lessonTimes.setText(String.format("%s - %s", timeLord.getLessonTime(lesson.getStartTime()), timeLord.getLessonTime(lesson.getEndTime())));
        }else
            holder.lessonTimes.setText(holder.itemView.getContext().getString(R.string.time_unknown));

        if(lesson.hasHomework()){
            Homework homework = lesson.getHomework();

            if(homework.hasTasks()) {
                StringBuilder homeworkBuilder = new StringBuilder();

                int t = 0;
                for (Hometask task : homework.getTasks()) {
                    homeworkBuilder.append("● ");
                    homeworkBuilder.append(task.getTask());
                    if (t++ < lesson.getHomework().getTasks().size()-1)
                        homeworkBuilder.append("\n");
                }

                holder.lessonHomework.setText(homeworkBuilder.toString());
            }else {
                holder.lessonHomework.setText(holder.itemView.getContext().getString(R.string.no_homework));
            }

            if(homework.hasAttachments()){
                //Show and populate attachments container !!
            }else {
                //Hide attachments container !!
            }

        }else {
            holder.lessonHomework.setText(holder.itemView.getContext().getString(R.string.no_homework));
            //Hide attachments container !!
        }

        if(lesson.hasMarks()){
            holder.marksContainer.removeAllViews();

            for(final Mark mark: lesson.getMarks()){
                View markView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.diary_day_lesson_mark, null);
                final Button markButton = (Button) markView.findViewById(R.id.markButton);
                markButton.setText(mark.getValue());

                if(mark.hasComment()){
                    markButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AlertDialog.Builder(holder.itemView.getContext()).setMessage(mark.getComment()).create().show();
                        }
                    });
                }else {
                    markButton.setBackgroundColor(Color.TRANSPARENT);
                }

                holder.marksContainer.addView(markView);
            }
        }else {
            holder.marksContainer.setVisibility(View.GONE);
            /*final int originalHeight = holder.marksContainer.getHeight();
            ValueAnimator heightAnimator = ValueAnimator.ofFloat(originalHeight, 0);
            heightAnimator.setDuration(100);
            heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.marksContainer.getLayoutParams();
                    params.height = (int) ((float)valueAnimator.getAnimatedValue());
                    holder.marksContainer.setLayoutParams(params);
                }
            });
            heightAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    holder.marksContainer.setVisibility(View.GONE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.marksContainer.getLayoutParams();
                    params.height = originalHeight;
                    holder.marksContainer.setLayoutParams(params);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            heightAnimator.start();*/

        }
    }

    @Override
    public int getItemCount() {
        return day==null?0:day.getLessons().size();
    }

    class LessonViewHolder extends RecyclerView.ViewHolder{
        private TextView lessonNumber;
        private TextView lessonName;
        private TextView lessonTimes;
        private TextView lessonHomework;

        private LinearLayout marksContainer;
        public LessonViewHolder(View itemView) {
            super(itemView);
            lessonNumber = (TextView) itemView.findViewById(R.id.lessonNumber);
            lessonName = (TextView) itemView.findViewById(R.id.lessonTitle);
            lessonTimes = (TextView) itemView.findViewById(R.id.lessonTimes);
            lessonHomework = (TextView) itemView.findViewById(R.id.lessonHomework);
            marksContainer = (LinearLayout) itemView.findViewById(R.id.marksContainer);
        }
    }
}
