package com.example.parrotronicandroid;

public interface PlayerActivity {

    void stopPlaying();
    void pausePlaying();
    void onPlay(boolean mStartPlaying, AudioNote note);

}
