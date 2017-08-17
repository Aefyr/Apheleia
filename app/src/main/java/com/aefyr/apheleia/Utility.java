package com.aefyr.apheleia;

import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by Aefyr on 14.08.2017.
 */

public class Utility {
    public static float dpToPx(int dp, Resources r){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static float displayWidth(Resources r){
        return r.getDisplayMetrics().widthPixels;
    }

    public static boolean checkSelectedTime(int i, String[] a){
        return i<a.length&&i>=0;
    }

    public static int clamp(int a, int min, int max){
        if(a<min)
            return min;
        if(a>max)
            return max;
        return a;
    }
}
