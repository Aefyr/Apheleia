package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class FinalPeriod implements Serializable {
    String name;
    String value;
    String comment;

    public String getName() {
        return name;
    }

    public String getMark() {
        return value;
    }

    public boolean hasComment() {
        return comment != null;
    }

    public String getComment() {
        return comment;
    }
}
