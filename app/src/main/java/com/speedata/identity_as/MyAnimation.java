package com.speedata.identity_as;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

/**
 * Created by brxu on 2017/3/21.
 */

public class MyAnimation {
    public static void showLogoAnimation(Context context, View view){
        Animation operatingAnim = AnimationUtils.loadAnimation(context, R.anim.logo);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        view.startAnimation(operatingAnim);
    }
}
