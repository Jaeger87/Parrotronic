package com.example.parrotronicandroid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class AudioNoteAdapter extends RecyclerView.Adapter<AudioNoteAdapter.ViewHolder>{


    private List<AudioNote> mData;
    private LayoutInflater mInflater;

    private Context ctx;

    public AudioNoteAdapter(List<AudioNote> mData, Context ctx)
    {
        this.mData = mData;
        this.ctx = ctx;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{



        ViewHolder(View itemView) {
            super(itemView);
        }
    }

}
