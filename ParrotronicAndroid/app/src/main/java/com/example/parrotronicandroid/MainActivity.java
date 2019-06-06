package com.example.parrotronicandroid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.parrotronicandroid.utilities.Constants;
import com.example.parrotronicandroid.utilities.StaticMethods;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements BTHeadActivity, PlayerActivity{

    private RecordButton recordButton = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    private static final String TAG = "MainParrot";

    public static final int amplitudePeriod = 70;
    public static final int amplitudeDelay = 170;


    private Timer _timer;
    private TimerForRecorder timeTask;

    private WaveFormUpdater waveFormUpdater;

    private Handler mWaveFormUpdateHandler;

    private List<AudioNote> audioNotes;
    private AudioNoteAdapter mAdapter;

    //From Renato
    private BluetoothConnectionService mBluetoothConnectionHead;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBTDeviceHead;

    private Gson gson;

    private Executor myExecutor;

    private Vibrator vibe;

    @ViewById(R.id.mouthValueText)
    TextView mouthValueTextView;

    @ViewById(R.id.timerForRecording)
    TextView timerTextView;

    @ViewById(R.id.mouthBar)
    SeekBar mouthBar;

    @ViewById(R.id.eyesSwitch)
    Switch eyes;

    @ViewById(R.id.autoScalingSwitch)
    Switch autoScallingSwitch;

    boolean autoScalling;

    @ViewById(R.id.micfab)
    FloatingActionButton micFab;

    @ViewById(R.id.recyclerAudio)
    protected RecyclerView audioNotesContainer;



    @AfterViews
    void AfterViews(){

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter((Constants.incomingMessageIntent)));

        autoScallingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoScalling = isChecked;
            }
        });


        eyes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                eyesSwitchPressed(isChecked);
            }
        });

        mouthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mouthValueTextView.setText(""+ progress);
                sendToHeadValueMouth(progress, true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);


        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        recordButton = new RecordButton(this, micFab);

        myExecutor = Executors.newFixedThreadPool(7);

        gson = new Gson();

        String saveFilePath = this.getFilesDir() + Constants.SaveFileName;

        try
        {
           String json = StaticMethods.readFile(saveFilePath, Charset.defaultCharset());
           Type gsonType = new TypeToken<ArrayList<AudioNote>>(){}.getType();
           audioNotes = gson.fromJson(json, gsonType);
           //waveform.updateVisualizer(convertBytes(audioNotes.get(audioNotes.size() -1 ).getAmplitudeGraphicList()));
        }

        catch (IOException e)
        {
            audioNotes = new ArrayList<>();
        }

        mAdapter = new AudioNoteAdapter(audioNotes, this,this);

        audioNotesContainer.setLayoutManager(new LinearLayoutManager(this));
        audioNotesContainer.setAdapter(mAdapter);
        connectionBluetooth();

    }

    private void connectionBluetooth()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bt : pairedDevices)
        {
            Log.d(TAG, bt.getAddress());

            if (bt.getAddress().equals(Constants.macHeadBT)) {
                Log.d(TAG, bt.getName());

                mBTDeviceHead = bt;
                mBluetoothConnectionHead = new BluetoothConnectionService(MainActivity.this, Constants.HeadID);
                startBTConnection(mBTDeviceHead, mBluetoothConnectionHead);
            }

        }
    }

    public void startBTConnection(BluetoothDevice device, BluetoothConnectionService connection)
    {
        connection.startClient(device);
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            stopPlaying();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(timeTask != null)
            timeTask.cancel(true);

        if(mBluetoothConnectionHead!= null) {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mReceiver);
            mBluetoothConnectionHead.stopClient();
        }
    }



    private void eyesSwitchPressed(boolean isChecked)
    {
        sendToHeadEyes(isChecked);
    }


    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void onPlay(boolean start, AudioNote audioNote) {
        if (start) {
            micFab.setImageResource(R.drawable.ic_stop);
            startPlaying(audioNote);
        } else {
            pausePlaying();
        }
    }


    private void startPlaying(AudioNote audioNote) {

        if(player == null) {
            player = new MediaPlayer();
            try {
                player.setDataSource(audioNote.getFileName());
                player.prepare();


                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {


                        // stop streaming vocal note
                        if (player != null) {
                            stopPlaying();
                        }
                        mAdapter.audioNoteFinished();
                    }
                });

                mWaveFormUpdateHandler = new Handler();
                waveFormUpdater = new WaveFormUpdater(player, mWaveFormUpdateHandler, audioNote, this, mAdapter.getCurrentWaveForm(), autoScalling);

                mWaveFormUpdateHandler.postDelayed(waveFormUpdater, 200);

            } catch (IOException e) {
                Log.e(TAG, "prepare() failed");
            }
        }

        player.start();
    }

    public void stopPlaying() {
        if(player != null) {
            mAdapter.stopPressed();
            mWaveFormUpdateHandler.removeCallbacks(waveFormUpdater);
            player.stop();
            player.release();
            player = null;
            micFab.setImageResource(R.drawable.ic_mic);
        }
    }

    public void pausePlaying() {
        player.pause();
        micFab.setImageResource(R.drawable.ic_mic);
    }


    private AudioNote currentAudioNoteInRecording;
    private void startRecording() {
        vibe.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE));
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        final AudioNote noteToRecorder = new AudioNote(getExternalCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".3gp");

        currentAudioNoteInRecording = noteToRecorder;

        recorder.setOutputFile(noteToRecorder.getFileName());

        recorder.setAudioSamplingRate(22050);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioChannels(2);

        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        _timer = new Timer();

        _timer.schedule(new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int amplitude = recorder.getMaxAmplitude();
                       // Log.d(TAG, "" + amplitude);
                        //Log.i(TAG, "" + map(amplitude, 0, 32762,0,255));
                        noteToRecorder.addToAmplitudeGraphicList((byte)StaticMethods.map(amplitude, 0, 32762,0,255));
                        noteToRecorder.addToAmplitudeAnalogicList((int)StaticMethods.map(amplitude,0,32762,0,1023));
                    }
                });
            }
        },amplitudeDelay,amplitudePeriod);

        timeTask = new TimerForRecorder(currentAudioNoteInRecording);
        timeTask.executeOnExecutor(myExecutor);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
    }


    private void stopRecording() {
        _timer.cancel();
        recorder.stop();
        timeTask.stopTimer();
        recorder.release();
        recorder = null;
        timeTask = null;
        vibe.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE));
    }


    @Override
    public void sendToHeadValueMouth(int value, boolean thanos) {

        if(mBluetoothConnectionHead == null)
            return;

        char thanosChar = 'T';

        if(!thanos)
            thanosChar = 'A';

        Log.d("BTHead", "M;" + thanosChar + ';' + value + "\n");

        if(mBluetoothConnectionHead != null)
            mBluetoothConnectionHead.write(("M;" + thanosChar + ';' + value + "\n").getBytes(Charset.defaultCharset()));
    }

    public void sendToHeadEyes(boolean onOff) {

        if(mBluetoothConnectionHead == null)
            return;

        char eyesChar = '1';

        if(!onOff)
            eyesChar = '0';

        mBluetoothConnectionHead.write(("E;" + eyesChar + "\n").getBytes(Charset.defaultCharset()));
    }

    class RecordButton{
        Context ctx;
        boolean mStartRecording = true;

        View.OnClickListener clicker = new View.OnClickListener() {
            public void onClick(View v) {

                if(player != null)
                {
                    stopPlaying();
                    return;
                }


                onRecord(mStartRecording);

                if (mStartRecording) {
                    micFab.setImageResource(R.drawable.ic_stop);
                } else {
                    micFab.setImageResource(R.drawable.ic_mic);
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx, FloatingActionButton button) {
            this.ctx = ctx;
            button.setOnClickListener(clicker);
        }
    }


    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("Message");

            if(text.contains("ALI"))
                return;

        }
    };



    public class TimerForRecorder extends AsyncTask<String, String, String> {

        private AudioNote audioNote;
        private boolean stop = false;

        public TimerForRecorder(AudioNote audioNote)
        {
            this.audioNote = audioNote;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            timerTextView.setText("00:00");
            timerTextView.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            long startTime = System.currentTimeMillis();
            int seconds = 0;
            int minutes = 0;
            while(!stop)
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long timePassed = System.currentTimeMillis() - startTime;
                timePassed /= 1000;
                seconds = (int) timePassed % 60;
                minutes = (int) timePassed / 60;

                publishProgress(printNumberForTimer(minutes) + ":" + printNumberForTimer(seconds));
            }
            return (printNumberForTimer(minutes) + ":" + printNumberForTimer(seconds));
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            timerTextView.setText(values[0]);
        }

        private String printNumberForTimer(int n)
        {
            if(n > 9)
                return ""+n;
            return "0"+n;
        }

        public void stopTimer()
        {
            stop = true;
        }

        @Override
        protected void onPostExecute(String result) {
            timerTextView.setVisibility(View.GONE);
            audioNote.setDurata(result);
            addAudioNote(audioNote);
        }

    }

    private void addAudioNote(AudioNote audioNote)
    {
        audioNotes.add(audioNote);
        mAdapter.notifyItemInserted(audioNotes.size() - 1);
        saveMe();
    }

    @Override
    public void deleteNote(int indexNote) {

        if(player != null)
            stopPlaying();

        File f = new File(audioNotes.get(indexNote).getFileName());
        f.delete();
        audioNotes.remove(indexNote);
        mAdapter.notifyItemRemoved(indexNote);
        saveMe();
    }

    private void saveMe()
    {
        String json = gson.toJson(audioNotes);

        try (PrintWriter out = new PrintWriter(this.getFilesDir() + Constants.SaveFileName)) {
            out.println(json);
        }

        catch (FileNotFoundException fnf)
        {

        }
    }

}
