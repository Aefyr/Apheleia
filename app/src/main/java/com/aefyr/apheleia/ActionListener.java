package com.aefyr.apheleia;

/**
 * Created by Aefyr on 22.08.2017.
 */

public interface ActionListener {
    public enum Action{
        STUDENT_SWITCHED, UPDATE_REQUESTED
    }

    public void onAction(Action action);
}
