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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parrotronicandroid.utilities.Constants;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.example.parrotronicandroid.utilities.StaticMethods.convertBytes;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements BTHeadActivity, PlayerActivity{

    private RecordButton recordButton = null;
    private PlayButton   playButton = null;

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

    private AudioNote audioNote;

    private List<AudioNote> audioNotes;
    private AudioNoteAdapter mAdapter;

    //From Renato
    private BluetoothConnectionService mBluetoothConnectionHead;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBTDeviceHead;

    private Executor myExecutor;

    @ViewById(R.id.mouthValueText)
    TextView mouthValueTextView;

    @ViewById(R.id.mouthBar)
    SeekBar mouthBar;

    @ViewById(R.id.eyesSwitch)
    Switch eyes;

    @ViewById(R.id.micfab)
    FloatingActionButton micFab;

    @ViewById(R.id.playFab)
    FloatingActionButton playFab;

    @ViewById(R.id.durataVoice)
    TextView durataVoice;

    @ViewById(R.id.voiceCard)
    CardView voiceCard;

    @ViewById(R.id.waveform)
    PlayerVisualizerView waveform;


    @AfterViews
    void AfterViews(){

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter((Constants.incomingMessageIntent)));

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

        recordButton = new RecordButton(this, micFab);

        playButton = new PlayButton(this, playFab);

        myExecutor = Executors.newFixedThreadPool(7);


        voiceCard.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Log.d(TAG, "lungo click"); //TODO pop up
                return true;
            }
        });


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
            if(!playButton.mStartPlaying) {
                playButton.mStartPlaying = false;
                playFab.callOnClick();
            }
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

        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mReceiver);
        mBluetoothConnectionHead.stopClient();
    }



    private void eyesSwitchPressed(boolean isChecked)
    {
        Toast.makeText(this, "premuto " + isChecked, Toast.LENGTH_LONG).show();
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
            startPlaying(audioNote.getFileName());
        } else {
            pausePlaying();
        }
    }

    private void startPlaying(String filename) {

        if(player == null) {
            player = new MediaPlayer();
            try {
                player.setDataSource(filename);
                player.prepare();


                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {

                        playFab.setImageResource(R.drawable.ic_play);
                        // stop streaming vocal note
                        if (player != null) {
                            stopPlaying();
                        }
                        playButton.mStartPlaying = true;
                    }
                });


                mWaveFormUpdateHandler = new Handler();
                waveFormUpdater = new WaveFormUpdater(player, mWaveFormUpdateHandler, audioNote, this, waveform);

                mWaveFormUpdateHandler.postDelayed(waveFormUpdater, 200);

            } catch (IOException e) {
                Log.e(TAG, "prepare() failed");
            }
        }

        player.start();
    }

    public void stopPlaying() {
        if(player != null) {
            mWaveFormUpdateHandler.removeCallbacks(waveFormUpdater);
            player.stop();
            player.release();
            player = null;
            micFab.setImageResource(R.drawable.ic_mic);
            waveform.updatePlayerPercent(0);
        }
    }

    public void pausePlaying() {
        player.pause();
        micFab.setImageResource(R.drawable.ic_mic);
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        audioNote = new AudioNote(getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp");

        recorder.setOutputFile(audioNote.getFileName());

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
                        audioNote.addToAmplitudeGraphicList((byte)map(amplitude, 0, 32762,0,255));
                        audioNote.addToAmplitudeAnalogicList((int)map(amplitude,0,32762,0,1023));
                    }
                });
            }
        },amplitudeDelay,amplitudePeriod);

        timeTask = new TimerForRecorder(audioNote);
        timeTask.executeOnExecutor(myExecutor);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
    }


    //Thank you processing
    static public final float map(float value,
                                  float istart,
                                  float istop,
                                  float ostart,
                                  float ostop) {
        return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
    }


    private void stopRecording() {
        _timer.cancel();
        recorder.stop();
        timeTask.stopTimer();
        recorder.release();
        recorder = null;


        timeTask = null;

        waveform.updateVisualizer(convertBytes(audioNote.getAmplitudeGraphicList()));

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

                if(!playButton.mStartPlaying)
                {
                    stopPlaying();

                    playFab.setImageResource(R.drawable.ic_play);
                    playButton.mStartPlaying = !playButton.mStartPlaying;
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

    class PlayButton{
        boolean mStartPlaying = true;
        Context ctx;

        View.OnClickListener clicker = new View.OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying, audioNote);
                if (mStartPlaying) {
                    playFab.setImageResource(R.drawable.ic_pause);
                } else {
                    playFab.setImageResource(R.drawable.ic_play);
                }
                mStartPlaying = !mStartPlaying;

            }
        };

        public PlayButton(Context ctx, FloatingActionButton button) {
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



    public class TimerForRecorder extends AsyncTask<String, Integer, String> {

        private AudioNote audioNote;
        private boolean stop = false;

        public TimerForRecorder(AudioNote audioNote)
        {
            this.audioNote = audioNote;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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

            }
            return (printNumberForTimer(minutes) + ":" + printNumberForTimer(seconds));
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
            audioNote.setDurata(result);
            durataVoice.setText(audioNote.getDurata());
        }



    }

    private void addAudioNote(AudioNote audioNote)
    {
        audioNotes.add(audioNote);
        mAdapter.notifyItemInserted(audioNotes.size() - 1);
    }

}
