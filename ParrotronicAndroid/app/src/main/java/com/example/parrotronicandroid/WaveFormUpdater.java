package com.example.parrotronicandroid;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import com.example.parrotronicandroid.utilities.StaticMethods;

public class WaveFormUpdater implements Runnable{

    private static final String TAG = "WaveFormUpdater";
    private MediaPlayer player;
    private Handler mWaveFormUpdateHandler;
    private PlayerVisualizerView playerVisualizerView;
    private AudioNote note;
    private BTHeadActivity activity;
    private int maxValue = 1023;



    public WaveFormUpdater(MediaPlayer player, Handler mWaveFormUpdateHandler, AudioNote note, BTHeadActivity activity, PlayerVisualizerView playerVisualizerView, boolean autoScalling)
    {
        this.player = player;
        this.mWaveFormUpdateHandler = mWaveFormUpdateHandler;
        this.note = note;
        this.activity = activity;
        this.playerVisualizerView = playerVisualizerView;

        if(autoScalling)
            maxValue = note.getMaxAnalogicValue();
    }



    @Override
    public void run() {
        float percent = (player.getCurrentPosition()) / (float) player.getDuration();
        playerVisualizerView.updatePlayerPercent(percent);

        int index = (int)((note.getAmplitudeAnalogicList().size() - 1 ) * percent );

        activity.sendToHeadValueMouth((int)StaticMethods.map(note.getAmplitudeAnalogicList().get(index), 0, 1023, 0, maxValue),false);
        mWaveFormUpdateHandler.postDelayed(this, MainActivity.amplitudePeriod);


    }
}
