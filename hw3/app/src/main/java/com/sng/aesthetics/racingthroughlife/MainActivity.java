package com.sng.aesthetics.racingthroughlife;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor mAccelerometer, mGravity, mStep;
    private StepCounter stepCounter;

    private int heightInches = 66;

    float[] gravity;

    private ArrayList<Double> pastAcc;
    private int REQUEST_CODE = 1;
    MediaProjectionManager mediaProjectionManager;

    private TextView accDisplay;
    private TextView stepDisplay;
    private Button musicSelector;

    private MediaPlayer mediaPlayer;
    private MediaController mediaController;

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
                    Log.i("uri", file.toString());


                    try {
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(getApplicationContext(), file);
                        mediaPlayer.prepareAsync();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gravity = new float[3];
        pastAcc = new ArrayList<>();
        stepCounter = new StepCounter();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mStep = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // accDisplay = findViewById(R.id.accel_display);
        stepDisplay = findViewById(R.id.step_display);
        musicSelector = findViewById(R.id.musicSelector);
        mediaController = findViewById(R.id.musicPlayer);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();

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

    }

    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_GRAVITY) {
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];

        } else if (sensorType == Sensor.TYPE_ACCELEROMETER){
            double[] linearAcc = new double[3];

            // Remove the gravity contribution with the high-pass filter.
            linearAcc[0] = event.values[0] - gravity[0];
            linearAcc[1] = event.values[1] - gravity[1];
            linearAcc[2] = event.values[2] - gravity[2];

            double sumSquares = 0.0;

            for (int i = 0; i < linearAcc.length; i++) {
                sumSquares += Math.pow(linearAcc[i], 2);
            }
            double netAcc = Math.sqrt(sumSquares);

            if (pastAcc.size() > 10) {
                pastAcc.remove(0);
            }

            pastAcc.add(netAcc);

            double avgAcc = pastAcc.stream().mapToDouble(d -> d).average().orElse(0.0);

//            String displayText = "X: " + linearAcc[0] + "\nY: " + linearAcc[1] + "\nZ: " +
//                    linearAcc[2] + "\nAvg net: " + pastAcc;
//            accDisplay.setText(displayText);
        } else if (sensorType == Sensor.TYPE_STEP_DETECTOR) {
            stepCounter.increment();

        }

        double avgSteps = stepCounter.getRollingAvgSteps();
        double feetPerSecond = heightInches * 0.415 / 12 / 60 * avgSteps;
        String displayText = "Total Steps: " + stepCounter.getTotalSteps()
                + "\nAvg. Steps:" + avgSteps
                + "\nAvg. Speed:" + feetPerSecond + " ft/s";

        setPlaybackSpeed(feetPerSecond);

        stepDisplay.setText(displayText);
    }

    private void setPlaybackSpeed(double feetPerSecond) {
        if (mediaPlayer.isPlaying()) {
            double speed = 3.0 / (1 + Math.pow(Math.E, -0.25 * (feetPerSecond - 6)));
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed((float) speed));
        }
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
