package com.aefyr.apheleia.viewmodels;

import com.aefyr.journalism.exceptions.JournalismException;

/**
 * Created by Aefyr on 20.04.2018.
 */
public abstract class ApheleiaDataState<T> {
    public static final int NOT_READY = 0;
    public static final int UPDATING = 1;
    public static final int OK_CACHED_PRELOAD = 2;
    public static final int OK = 3;
    public static final int NET_ERROR = 4;
    public static final int API_ERROR = 5;
    public static final int TOKEN_DEAD = 6;

    protected int state = 0;
    protected T data;
    protected JournalismException error;

    public int getState(){
        return state;
    }

    public T getData(){
        return data;
    }

    public void setData(T data){
        this.data = data;
        state = OK;
    }

    public void setDataFromCache(T data){
        this.data = data;
        state = OK_CACHED_PRELOAD;
    }

    public void setUpdating(){
        state = UPDATING;
    }

    public void setNetError(){
        state = NET_ERROR;
    }

    public void setApiError(JournalismException e){
        error = e;
        state = API_ERROR;
    }

    public JournalismException getApiErrorInfo(){
        return error;
    }

    public void setTokenDead(){
        state = TOKEN_DEAD;
    }


}
