package com.aefyr.journalism.objects.minor;

import java.io.Serializable;

public class MessageReceiver extends MessagePerson implements Serializable {
	String info;
	
	public String getInfo(){
		return info;
	}
	
	public boolean hasInfo(){
		return info!=null;
	}

	@Override
	public boolean equals(Object obj) {
		return obj.getClass() == getClass() && getId().equals(((MessageReceiver) obj).getId());
	}

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
