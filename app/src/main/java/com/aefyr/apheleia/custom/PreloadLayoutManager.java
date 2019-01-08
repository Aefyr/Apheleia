package com.aefyr.apheleia.custom;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Aefyr on 15.08.2017.
 */

public class PreloadLayoutManager extends LinearLayoutManager {
    private int preloadPagesCount = 2;

    public PreloadLayoutManager(Context context, int preloadPagesCount) {
        super(context);
        this.preloadPagesCount = preloadPagesCount;
    }

    @Override
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        return getWidth() * preloadPagesCount;
    }
}
