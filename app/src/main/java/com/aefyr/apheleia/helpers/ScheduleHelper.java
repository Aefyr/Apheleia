package com.aefyr.apheleia.helpers;

import android.content.Context;

import com.aefyr.journalism.objects.major.Schedule;

/**
 * Created by Aefyr on 15.08.2017.
 */

public class ScheduleHelper extends SerializerHelperWithTimeAndStudentKeysBase<Schedule> {
    private static ScheduleHelper instance;

    private ScheduleHelper(Context c) {
        super(c);
        instance = this;
    }

    @Override
    protected String getFolderName() {
        return "schedule";
    }

    @Override
    protected String getExtension() {
        return ".as";
    }

    public static ScheduleHelper getInstance(Context c) {
        return instance == null ? new ScheduleHelper(c) : instance;
    }

    public boolean isScheduleSaved(String weeks) {
        return isObjectSaved(weeks);
    }


    public boolean saveSchedule(Schedule schedule, String weeks) {
        return saveObject(schedule, weeks);
    }

    public Schedule loadSavedSchedule(String weeks) throws Exception {
        return loadSavedObject(weeks);
    }


    public void saveScheduleAsync(Schedule schedule, String weeks, ObjectSaveListener listener) {
        saveObjectAsync(schedule, weeks, listener);
    }

    static void destroy() {
        instance = null;
    }

}
