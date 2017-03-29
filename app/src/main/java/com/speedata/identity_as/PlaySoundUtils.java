package com.speedata.identity_as;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by brxu on 2017/3/21.
 */

public class PlaySoundUtils {

    private static Map<Integer, Integer> mapSRC;
    private static SoundPool sp; //声音池

    //初始化声音池
    public static void initSoundPool(Context context) {
        sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mapSRC = new HashMap<Integer, Integer>();
        mapSRC.put(1, sp.load(context, R.raw.error, 0));
        mapSRC.put(2, sp.load(context, R.raw.welcome, 0));
        mapSRC.put(3, sp.load(context, R.raw.msg, 0));
    }

    /**
     * 播放声音池的声音
     */
    public static void play(int sound, int number) {
        sp.play(mapSRC.get(sound),//播放的声音资源
                1.0f,//左声道，范围为0--1.0
                1.0f,//右声道，范围为0--1.0
                0, //优先级，0为最低优先级
                number,//循环次数,0为不循环
                0);//播放速率，0为正常速率
    }
}
