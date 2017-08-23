package com.aefyr.apheleia.helpers;

import android.content.Context;

import com.aefyr.journalism.objects.major.Finals;

/**
 * Created by Aefyr on 21.08.2017.
 */

public class FinalsHelper extends SerializerHelperWithTimeAndStudentKeysBase<Finals> {
    private static FinalsHelper instance;

    private FinalsHelper(Context c) {
        super(c);
        instance = this;
    }

    public static FinalsHelper getInstance(Context c) {
        return instance == null ? new FinalsHelper(c) : instance;
    }

    @Override
    protected String getFolderName() {
        return "finals";
    }

    @Override
    protected String getExtension() {
        return ".afm";
    }

    public Finals loadFinals() throws Exception {
        return loadSavedObject("");
    }

    public void saveFinalsAsync(Finals finals, ObjectSaveListener listener) {
        saveObjectAsync(finals, "", listener);
    }

    public boolean saveFinals(Finals finals) {
        return saveObject(finals, "");
    }

    static void destroy() {
        instance = null;
    }
}
