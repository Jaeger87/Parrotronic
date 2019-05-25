package com.example.parrotronicandroid;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

public class WaveFormUpdater implements Runnable{

    private static final String TAG = "WaveFormUpdater";
    private PlayerVisualizerView visualizerView;
    private MediaPlayer player;
    private Handler mWaveFormUpdateHandler;



    public WaveFormUpdater(PlayerVisualizerView visualizerView, MediaPlayer player, Handler mWaveFormUpdateHandler)
    {
        this.visualizerView = visualizerView;
        this.player = player;
        this.mWaveFormUpdateHandler = mWaveFormUpdateHandler;
    }

    @Override
    public void run() {
        float percent = (player.getCurrentPosition()) / (float) player.getDuration();
        Log.d(TAG, "" + percent);
        visualizerView.updatePlayerPercent(percent);
        mWaveFormUpdateHandler.postDelayed(this, 100);


    }
}
