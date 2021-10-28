package com.sng.aesthetics.racingthroughlife;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.MediaController;

import java.io.IOException;

public class AudioPlayer implements MediaController.MediaPlayerControl {

    private MediaPlayer mediaPlayer;
    private Context context;
    private double MAX_SPEED = 3.0;

    private int mCurrentBufferPercent;

    public AudioPlayer(Context context) {
        mediaPlayer = new MediaPlayer();
        this.context = context;

        mCurrentBufferPercent = 0;


        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> mCurrentBufferPercent = percent);
        mediaPlayer.setOnPreparedListener(mediaPlayer -> mediaPlayer.start());


    }

    public void attachMediaController(MediaController controller, View view) {
        controller.setMediaPlayer(this);
        controller.setEnabled(true);
        controller.setAnchorView(view);

        controller.show();
    }

    public void setTrack(Uri uri) {
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, uri);


            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPlaybackSpeed(double feetPerSecond) {
        if (mediaPlayer.isPlaying()) {
            double shift = 0.5;
            double speed = (MAX_SPEED - shift) / (1 + Math.pow(Math.E, -1.25 * (feetPerSecond - 5.5))) + shift;
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed((float) speed));
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {
        mediaPlayer.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return mCurrentBufferPercent;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }
}
