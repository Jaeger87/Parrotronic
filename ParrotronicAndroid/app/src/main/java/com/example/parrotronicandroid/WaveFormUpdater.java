package com.example.parrotronicandroid;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

public class WaveFormUpdater implements Runnable{

    private static final String TAG = "WaveFormUpdater";
    private MediaPlayer player;
    private Handler mWaveFormUpdateHandler;
    private PlayerVisualizerView playerVisualizerView;
    private AudioNote note;
    private BTHeadActivity activity;



    public WaveFormUpdater(MediaPlayer player, Handler mWaveFormUpdateHandler, AudioNote note, BTHeadActivity activity, PlayerVisualizerView playerVisualizerView)
    {
        this.player = player;
        this.mWaveFormUpdateHandler = mWaveFormUpdateHandler;
        this.note = note;
        this.activity = activity;
        this.playerVisualizerView = playerVisualizerView;
    }



    @Override
    public void run() {
        float percent = (player.getCurrentPosition()) / (float) player.getDuration();
        playerVisualizerView.updatePlayerPercent(percent);

        int index = (int)((note.getAmplitudeAnalogicList().size() - 1 ) * percent );

        activity.sendToHeadValueMouth(note.getAmplitudeAnalogicList().get(index),false);
        mWaveFormUpdateHandler.postDelayed(this, MainActivity.amplitudePeriod);

    }
}
