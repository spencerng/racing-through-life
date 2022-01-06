package com.sng.aesthetics.racingthroughlife;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.DecimalFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor mAccelerometer, mGravity, mStep;
    private StepCounter stepCounter;

    // height of user in inches
    private int HEIGHT_INCHES = 66;
    private double strideLength;


    private ArrayList<Uri> playlist;
    private int playlistPosition;

    float[] gravity;
    boolean mediaPlayerPrepared;

    private ArrayList<Double> pastAcc;
    private int REQUEST_CODE = 1;
    MediaProjectionManager mediaProjectionManager;

    private TextView accDisplay, songView, artistView,
            avgStepView, curSpeedView, dailySpeedView, percentFasterView, albumView;
    private Button musicSelector, playButton;

    private AudioPlayer audioPlayer;
    private MediaController mediaController;
    private boolean playing;

    private boolean canRecordAudio() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.i("result", ":" + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri file = data.getData();

                    playTrack(file);

                    buildPlaylist(file);
                }
            });

    private void buildPlaylist(Uri file) {

  //      DocumentFile files = DocumentFile.fromTreeUri(getApplicationContext(), file);

        playlist = new ArrayList<>();

//        for (DocumentFile song : files.listFiles()) {
//            Uri songUri = song.getUri();
//            if (getApplicationContext().getContentResolver().getType(songUri).contains("audio")
//                && !songUri.equals(file)) {
//                playlist.add(songUri);
//            }
//        }

        Collections.shuffle(playlist);
        playlist.add(0, file);
        playlistPosition = 0;
    }

    private void playTrack(Uri file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(getApplicationContext(), file);

        songView.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        artistView.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        albumView.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));

        playButton.setBackground(getDrawable(R.drawable.ic_baseline_pause_circle_filled_24));

        mediaPlayerPrepared = true;
        audioPlayer.setTrack(file);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playlist = new ArrayList<>();
        playlistPosition = 0;

        gravity = new float[3];
        pastAcc = new ArrayList<>();
        stepCounter = new StepCounter(15);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mStep = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        strideLength = HEIGHT_INCHES * 0.415;

        // accDisplay = findViewById(R.id.accel_display);

        musicSelector = findViewById(R.id.musicSelector);
        songView = findViewById(R.id.songTitle);
        artistView = findViewById(R.id.artist);
        curSpeedView = findViewById(R.id.speedView);
        avgStepView = findViewById(R.id.stepView);
        dailySpeedView = findViewById(R.id.dailyAvgSpeedView);
        percentFasterView = findViewById(R.id.percentFasterView);
        albumView = findViewById(R.id.album);
        playButton = findViewById(R.id.playButton);

        // mediaController = findViewById(R.id.musicPlayer);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayerPrepared && !audioPlayer.isPlaying()) {
                    playButton.setBackground(getDrawable(R.drawable.ic_baseline_pause_circle_filled_24));
                    audioPlayer.start();
                } else {
                    playButton.setBackground(getDrawable(R.drawable.ic_baseline_play_circle_filled_24));
                    audioPlayer.pause();
                }

            }
        });

        audioPlayer = new AudioPlayer(getApplicationContext());
        mediaPlayerPrepared = false;

        audioPlayer.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playButton.setBackground(getDrawable(R.drawable.ic_baseline_play_circle_filled_24));
                audioPlayer.pause();
                mediaPlayerPrepared = false;
                artistView.setText("");
                albumView.setText("");
                songView.setText("Select a song to begin.");
                playlist = new ArrayList<>();
                playlistPosition = 0;
            }
        });


        musicSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("audio/*");
                chooseFile = Intent.createChooser(chooseFile, "Select a music file");

                activityResultLauncher.launch(chooseFile);

            }
        });


        if (!canRecordAudio()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_CODE
            );
        }

        mediaProjectionManager = (MediaProjectionManager)
                getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_CODE
        );

        Button backButton, skipButton;

        backButton = findViewById(R.id.backButton);
        skipButton = findViewById(R.id.nextButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playlistPosition >= 1) {
                    playlistPosition -= 1;
                    playTrack(playlist.get(playlistPosition));
                }

                if (playlistPosition < playlist.size() - 1) {
                    playlistPosition += 1;
                    playTrack(playlist.get(playlistPosition));
                }
            }
        });

    }

    public void updateStatDisplay() {

        stepCounter.cleanup();

        // cur avg steps, speed, social ranking, avg walking speed per day
        double avgStepsPerSec = stepCounter.getRollingAvgSteps();
        double feetPerSecond = strideLength / 12 * avgStepsPerSec;

        double cumAvgFeetPerSecond = strideLength / 12 * stepCounter.getCumulativeAvgStepsPerSec();

        DecimalFormat df = new DecimalFormat("#.#");
        avgStepView.setText(df.format(Math.round(avgStepsPerSec * 60)));
        curSpeedView.setText(df.format(feetPerSecond));
        // mean 4.4, 0.62 SD

        NormalDistribution dist = new NormalDistribution(4.4, 0.62);
        double percentFaster = Math.round(100 * dist.cumulativeProbability(cumAvgFeetPerSecond));

        dailySpeedView.setText(df.format(cumAvgFeetPerSecond / 5280 * 60 * 60));
        percentFasterView.setText((int) percentFaster + "%");

        audioPlayer.setPlaybackSpeed(feetPerSecond);

    }

    public void onSensorChanged(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_GRAVITY) {
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];

        } else if (sensorType == Sensor.TYPE_ACCELEROMETER) {

        } else if (sensorType == Sensor.TYPE_STEP_DETECTOR) {
            stepCounter.increment();

        }
        updateStatDisplay();

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mStep, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }


}
