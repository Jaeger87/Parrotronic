package com.example.parrotronicandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import static com.example.parrotronicandroid.utilities.StaticMethods.convertBytes;

public class AudioNoteAdapter extends RecyclerView.Adapter<AudioNoteAdapter.ViewHolder>{

    private static final String TAG = "AudioAdapter";

    private List<AudioNote> mData;
    private LayoutInflater mInflater;

    private PlayerActivity activity;

    private Context context;

    private ViewHolder currentViewHolderPlaying;

    public AudioNoteAdapter(List<AudioNote> mData, PlayerActivity activity, Context context)
    {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.activity = activity;
        this.context = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.audio_note, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bindAudioNote(mData.get(i), i);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void stopPressed()
    {
        if(currentViewHolderPlaying != null ) {
            if (!currentViewHolderPlaying.playButton.mStartPlaying) {
                currentViewHolderPlaying.playButton.mStartPlaying = false;
                currentViewHolderPlaying.playFab.callOnClick();
            }
            currentViewHolderPlaying.waveform.updatePlayerPercent(0);
        }
    }

    public void audioNoteFinished()
    {
        currentViewHolderPlaying.playFab.setImageResource(R.drawable.ic_play);
        currentViewHolderPlaying.playButton.mStartPlaying = true;
    }

    public AudioNote getCurrentAudioNote()
    {
        return currentViewHolderPlaying.audioNote;
    }


    public PlayerVisualizerView getCurrentWaveForm()
    {
        return currentViewHolderPlaying.waveform;
    }

    private AlertDialog AskOption(int index)
    {
        AlertDialog myQuittingDialogBox =new AlertDialog.Builder(context)
                //set message, title, and icon
                .setTitle("Delete")
                .setMessage("Do you want to Delete")
                .setIcon(R.drawable.delete)

                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, ""+ index);
                        activity.deleteNote(index);
                        dialog.dismiss();
                    }

                })



                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }
                })
                .create();
        return myQuittingDialogBox;

    }


    public class ViewHolder extends RecyclerView.ViewHolder{

        AudioNote audioNote;
        PlayerVisualizerView waveform;
        TextView durateTextView;
        FloatingActionButton playFab;
        CardView voiceCard;


        PlayButton playButton;

        ViewHolder(View itemView) {
            super(itemView);
            playFab = itemView.findViewById(R.id.playFab);
            durateTextView = itemView.findViewById(R.id.durataVoice);
            waveform = itemView.findViewById(R.id.waveform);
            voiceCard = itemView.findViewById(R.id.voiceCard);
            playButton = new PlayButton(playFab, this);


        }

        void bindAudioNote(AudioNote audioNote, int index)
        {
            this.audioNote = audioNote;

            durateTextView.setText(audioNote.getDurata());
            waveform.updateVisualizer(convertBytes(audioNote.getAmplitudeGraphicList()));

            voiceCard.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    AlertDialog diaBox = AskOption(index);
                    diaBox.show();
                    return true;
                }
            });
        }


        class PlayButton{
            boolean mStartPlaying = true;
            ViewHolder parent;
            View.OnClickListener clicker = new View.OnClickListener() {
                public void onClick(View v) {

                    if(currentViewHolderPlaying != parent)
                    {
                        activity.stopPlaying();
                        currentViewHolderPlaying = parent;
                    }

                    activity.onPlay(mStartPlaying, audioNote);
                    if (mStartPlaying) {
                        playFab.setImageResource(R.drawable.ic_pause);
                    } else {
                        playFab.setImageResource(R.drawable.ic_play);
                    }
                    mStartPlaying = !mStartPlaying;

                }
            };



            public PlayButton(FloatingActionButton button, ViewHolder parent) {
                button.setOnClickListener(clicker);
                this.parent = parent;
            }
        }
    }

}
