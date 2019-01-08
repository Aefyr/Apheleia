package com.aefyr.apheleia.fragments;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.aefyr.apheleia.R;
import com.aefyr.apheleia.utility.Utility;

/**
 * A simple {@link Fragment} subclass.
 */
public class MCMessageFragment extends Fragment {


    public MCMessageFragment() {
        // Required empty public constructor
    }

    private EditText subject;
    private EditText text;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mcmessage, container, false);

        subject = (EditText) view.findViewById(R.id.messageSubject);
        text = (EditText) view.findViewById(R.id.messageText);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (forcedSubject != null) {
            subject.setText(forcedSubject);
            text.requestFocus();
        }
    }

    public boolean checkFields() {
        if (subject.length() == 0) {
            Utility.highLightET(getResources(), subject);
            return false;
        }
        if (text.length() == 0) {
            Utility.highLightET(getResources(), text);
            return false;
        }

        return true;
    }

    public String getMessageSubject() {
        return subject.getText().toString();
    }

    public String getMessageText() {
        return text.getText().toString();
    }

    private String forcedSubject;

    public void setForcedMessageSubject(String subjectText) {
        forcedSubject = subjectText;
    }


}
