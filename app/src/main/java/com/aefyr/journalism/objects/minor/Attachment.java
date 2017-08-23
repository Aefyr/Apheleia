package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class Attachment implements Serializable {
    String name;
    String uri;

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }
}
