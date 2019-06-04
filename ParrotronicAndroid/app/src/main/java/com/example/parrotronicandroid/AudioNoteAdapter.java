package com.example.parrotronicandroid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import static com.example.parrotronicandroid.utilities.StaticMethods.convertBytes;

public class AudioNoteAdapter extends RecyclerView.Adapter<AudioNoteAdapter.ViewHolder>{


    private List<AudioNote> mData;
    private LayoutInflater mInflater;

    private PlayerActivity activity;

    private ViewHolder currentViewHolderPlaying;

    public AudioNoteAdapter(List<AudioNote> mData, PlayerActivity activity, Context context)
    {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.activity = activity;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.audio_note, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bindAudioNote(mData.get(i));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        AudioNote audioNote;
        PlayerVisualizerView playerVisualizerView;
        TextView durateTextView;
        FloatingActionButton playFab;

        PlayButton playButton;

        ViewHolder(View itemView) {
            super(itemView);


            playFab = itemView.findViewById(R.id.playFab);
            durateTextView = itemView.findViewById(R.id.durataVoice);
            playerVisualizerView = itemView.findViewById(R.id.waveform);
            playButton = new PlayButton(playFab);
        }

        void bindAudioNote(AudioNote audioNote)
        {
            this.audioNote = audioNote;
            durateTextView.setText(audioNote.getDurata());
            playerVisualizerView.updateVisualizer(convertBytes(audioNote.getAmplitudeGraphicList()));
        }


        class PlayButton{
            boolean mStartPlaying = true;
            View.OnClickListener clicker = new View.OnClickListener() {
                public void onClick(View v) {
                    activity.onPlay(mStartPlaying, audioNote);
                    if (mStartPlaying) {
                        playFab.setImageResource(R.drawable.ic_pause);
                    } else {
                        playFab.setImageResource(R.drawable.ic_play);
                    }
                    mStartPlaying = !mStartPlaying;

                }
            };

            public PlayButton(FloatingActionButton button) {
                button.setOnClickListener(clicker);
            }
        }
    }

}
