package com.aefyr.apheleia.utility;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.widget.EditText;

import com.aefyr.apheleia.R;

import java.io.File;

/**
 * Created by Aefyr on 14.08.2017.
 */

public class Utility {

    public static float dpToPx(int dp, Resources r) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static float displayWidthPx(Resources r) {
        return r.getDisplayMetrics().widthPixels;
    }

    public static int displayWidthDp(Resources r) {
        return (int) (displayWidthPx(r)/r.getDisplayMetrics().density);
    }

    public static boolean checkSelectedTime(int i, String[] a) {
        return i < a.length && i >= 0;
    }

    public static int clamp(int a, int min, int max) {
        if (a < min)
            return min;
        if (a > max)
            return max;
        return a;
    }

    public static void highLightET(Resources r, final EditText et) {
        ValueAnimator colorAnimator = new ValueAnimator();
        colorAnimator.setIntValues(Color.RED, r.getColor(R.color.colorEditTextHint));
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatCount(6);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.setDuration(100);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                et.setHintTextColor((Integer) valueAnimator.getAnimatedValue());
            }
        });
        et.requestFocus();
        colorAnimator.start();
    }

    public static void deleteRecursive(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteRecursive(f);
            }
        }
        file.delete();
    }

    public static void colorRefreshLayout(SwipeRefreshLayout refreshLayout) {
        refreshLayout.setColorSchemeColors(refreshLayout.getContext().getResources().getColor(R.color.colorAccent));
    }

}
