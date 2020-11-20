package com.sourabh.mitronassignment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import java.io.File;
import java.util.Objects;

import static com.sourabh.mitronassignment.utils.Constants.OUTPUT_FILE_NAME;
import static com.sourabh.mitronassignment.utils.Constants.PATH;
import static com.sourabh.mitronassignment.utils.Constants.VIDEO_URI;
import static com.sourabh.mitronassignment.utils.Constants.getPath;
import static com.sourabh.mitronassignment.utils.Constants.getRootDirPath;

public class VideoActivity extends BaseActivity implements Player.EventListener {

    private static final String TAG = "VideoActivity";
    private static final int AUDIO_REQUEST = 1;
    PlayerView videoFullScreenPlayer;
    ProgressBar spinnerVideoDetails;
    String videoPath,videoUri, audioPath;
    SimpleExoPlayer player;
    ProgressDialog progress;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow()
                .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video);
        videoFullScreenPlayer = findViewById(R.id.videoFullScreenPlayer);
        spinnerVideoDetails = findViewById(R.id.spinnerVideoDetails);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).hide();
        if (getIntent().hasExtra(VIDEO_URI)) {
            videoUri = getIntent().getStringExtra(VIDEO_URI);
            videoPath = getIntent().getStringExtra(PATH);
        }
        Log.e(TAG,"videoUri " + videoUri + " videoPath " + videoPath);
        Button select_audio = findViewById(R.id.select_audio);
        select_audio.setOnClickListener(v-> {
            openGalleryForAudio();
        });
        setUp();
    }
    private void setUp() {
        initializePlayer();
        if (videoPath == null) {
            return;
        }
        buildMediaSource(Uri.parse(videoPath));
    }


    private void initializePlayer() {
        if (player == null) {
            // 1. Create a default TrackSelector
            TrackSelection.Factory videoTrackSelectionFactory = new
                    AdaptiveTrackSelection.Factory();
            TrackSelector trackSelector =
                    new DefaultTrackSelector(videoTrackSelectionFactory);
            // 2. Create the player
            player =
                    ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(this),
                            trackSelector);
            videoFullScreenPlayer.setPlayer(player);
        }
    }
    private void buildMediaSource(Uri mUri) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter);
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mUri);
        // Prepare the player with the source.
        videoFullScreenPlayer.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        player.prepare(videoSource);
        player.seekTo(0);
        player.addListener(this);
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void pausePlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    private void resumePlayer() {
        if (player != null) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pausePlayer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        resumePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    public void openGalleryForAudio() {
        Intent videoIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(videoIntent, "Select Audio"), AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AUDIO_REQUEST){
            if(resultCode == RESULT_OK) {
                Uri audioUri = data.getData();
                Log.e(TAG," audioUri " + audioUri);
                try {
                    String uriString = audioUri.toString();
                    File myFile = new File(uriString);
                    audioPath = getPath(VideoActivity.this,audioUri);
                    File f = new File(audioPath);
                    long fileSizeInBytes = f.length();
                    long fileSizeInKB = fileSizeInBytes / 1024;
                    long fileSizeInMB = fileSizeInKB / 1024;
                    if (fileSizeInMB > 8) {
                        Toast.makeText(this, "Sorry file size is large", Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG,"audiopath " + audioPath + "f " + f.getAbsolutePath());
                    String appDirectoryPath = getRootDirPath(VideoActivity.this);
                    Log.e(TAG,"appDirectoryPath " + appDirectoryPath);
                    File outputPath = new File(appDirectoryPath,OUTPUT_FILE_NAME);
                    Log.e(TAG,"outputPath " + outputPath);
                    String[] command  = {"-i", videoPath, "-i", audioPath, "-c:v", "copy",
                            "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest",
                            outputPath.getAbsolutePath()};
                    execFFmpegBinary(command);
                } catch (Exception e) {
                    //handle exception
                    Toast.makeText(VideoActivity.this, "Unable to process,try again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.e(TAG, "FAILED with output : " + s);
                    Toast.makeText(VideoActivity.this, s, Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                }

                @Override
                public void onSuccess(String s) {
                    Log.e(TAG, "SUCCESS with output : " + s);
                    spinnerVideoDetails.setVisibility(View.GONE);
                    progress.dismiss();

                }
                @Override
                public void onProgress(String s) {
                    Log.e(TAG, "progress : " + s);
                  //  spinnerVideoDetails.setVisibility(View.VISIBLE);
                }

                @Override
                public void onStart() {
                    Log.e(TAG, "Started command : ffmpeg " + command);
                    progress = ProgressDialog.show(VideoActivity.this, "Please wait",
                            "Adding audio into the video", true);
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "Finished command : ffmpeg " + command);
                    progress.dismiss();
                    String appDirectoryPath = getRootDirPath(VideoActivity.this);
                    Log.e(TAG,"appDirectoryPath " + appDirectoryPath);
                    File outputPath = new File(appDirectoryPath,OUTPUT_FILE_NAME);
                    Log.e(TAG,"outputPath " + outputPath.getAbsoluteFile());
                    videoPath = outputPath.getAbsolutePath();
                    setUp();
                }
        });
    } catch (FFmpegCommandAlreadyRunningException e) {
        Log.e(TAG,"FFmpegCommandAlreadyRunningException " + e.getMessage());
    }
}
}