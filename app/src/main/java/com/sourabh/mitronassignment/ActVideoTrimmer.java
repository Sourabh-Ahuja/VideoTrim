package com.sourabh.mitronassignment;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.crystal.crystalrangeseekbar.widgets.CrystalSeekbar;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.sourabh.mitronassignment.utils.CompressOption;
import com.sourabh.mitronassignment.utils.CustomProgressView;
import com.sourabh.mitronassignment.utils.FileUtils;
import com.sourabh.mitronassignment.utils.LogMessage;
import com.sourabh.mitronassignment.utils.TrimVideo;
import com.sourabh.mitronassignment.utils.TrimVideoOptions;
import com.sourabh.mitronassignment.utils.TrimmerUtils;

import java.io.File;
import java.util.Objects;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static com.sourabh.mitronassignment.utils.Constants.OUTPUT_FILE_NAME;
import static com.sourabh.mitronassignment.utils.Constants.getPath;
import static com.sourabh.mitronassignment.utils.Constants.getRootDirPath;

public class ActVideoTrimmer extends BaseActivity {

    private static final String TAG = "ActVideoTrimmer";

    private static final int AUDIO_REQUEST = 1;
    private PlayerView playerView;

    private static final int PER_REQ_CODE = 115;

    private SimpleExoPlayer videoPlayer;

    private ImageView imagePlayPause;

    private ImageView[] imageViews;

    private long totalDuration;

    private Dialog dialog;

    private Uri uri;

    private TextView txtStartDuration, txtEndDuration;

    private CrystalRangeSeekbar seekbar;

    private long lastMinValue = 0;

    private long lastMaxValue = 0;

    private MenuItem menuDone;

    private CrystalSeekbar seekbarController;

    private boolean isValidVideo = true, isVideoEnded;

    private Handler seekHandler;

    private long currentDuration,lastClickedTime;

    private CompressOption compressOption;

    private String outputPath, destinationPath, audioPath;

    private int trimType;

    private long fixedGap, minGap, minFromGap, maxToGap;

    private boolean hidePlayerSeek, isAccurateCut;

    private CustomProgressView progressView;

    private String fileName;

    String appDirectoryPath;

    int number;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_video_trimmer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpToolBar(getSupportActionBar(), getString(R.string.txt_edt_video));
        toolbar.setNavigationOnClickListener(v -> finish());
        progressView = new CustomProgressView(this);
        appDirectoryPath = getRootDirPath(ActVideoTrimmer.this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        playerView = findViewById(R.id.player_view_lib);
        imagePlayPause = findViewById(R.id.image_play_pause);
        seekbar = findViewById(R.id.range_seek_bar);
        txtStartDuration = findViewById(R.id.txt_start_duration);
        txtEndDuration = findViewById(R.id.txt_end_duration);
        seekbarController = findViewById(R.id.seekbar_controller);
        Button audio_button = findViewById(R.id.audio_button);
        audio_button.setOnClickListener(v -> {
            addAudioToSelectPortion();
        });
        ImageView imageOne = findViewById(R.id.image_one);
        ImageView imageTwo = findViewById(R.id.image_two);
        ImageView imageThree = findViewById(R.id.image_three);
        ImageView imageFour = findViewById(R.id.image_four);
        ImageView imageFive = findViewById(R.id.image_five);
        ImageView imageSix = findViewById(R.id.image_six);
        ImageView imageSeven = findViewById(R.id.image_seven);
        ImageView imageEight = findViewById(R.id.image_eight);
        imageViews = new ImageView[]{imageOne, imageTwo, imageThree,
                imageFour, imageFive, imageSix, imageSeven, imageEight};
        seekHandler = new Handler();
        initPlayer();
        if (checkStoragePermission())
            setDataInView();
    }

    private void addAudioToSelectPortion() {
        Intent videoIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(videoIntent, "Select Audio"), AUDIO_REQUEST);
    }

    private void setUpToolBar(ActionBar actionBar, String title) {
        try {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayer() {
        try {
            videoPlayer =new SimpleExoPlayer.Builder(this).build();
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setPlayer(videoPlayer);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.CONTENT_TYPE_MOVIE)
                        .build();
                videoPlayer.setAudioAttributes(audioAttributes, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDataInView() {
        try {
            uri = Uri.parse(getIntent().getStringExtra(TrimVideo.TRIM_VIDEO_URI));
            uri = Uri.parse(FileUtils.getPath(this, uri));
            LogMessage.v("VideoUri:: " + uri);
            totalDuration = TrimmerUtils.getDuration(this, uri);
            imagePlayPause.setOnClickListener(v ->
                    onVideoClicked());
            Objects.requireNonNull(playerView.getVideoSurfaceView()).setOnClickListener(v ->
                    onVideoClicked());
            validate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validate() {
        try {
            TrimVideoOptions trimVideoOptions = getIntent().getParcelableExtra(TrimVideo.TRIM_VIDEO_OPTION);
            assert trimVideoOptions != null;
            trimType = TrimmerUtils.getTrimType(trimVideoOptions.trimType);
            destinationPath = trimVideoOptions.destination;
            hidePlayerSeek = trimVideoOptions.hideSeekBar;
            isAccurateCut = trimVideoOptions.accurateCut;
            compressOption = trimVideoOptions.compressOption;
            fixedGap = trimVideoOptions.fixedDuration;
            fixedGap = fixedGap != 0 ? fixedGap : totalDuration;
            minGap = trimVideoOptions.minDuration;
            minGap = minGap != 0 ? minGap : totalDuration;
            if (trimType == 3) {
                minFromGap = trimVideoOptions.minToMax[0];
                maxToGap = trimVideoOptions.minToMax[1];
                minFromGap = minFromGap != 0 ? minFromGap : totalDuration;
                maxToGap = maxToGap != 0 ? maxToGap : totalDuration;
            }
            if (destinationPath != null) {
                File outputDir = new File(destinationPath);
                outputDir.mkdirs();
                destinationPath = String.valueOf(outputDir);
                if (!outputDir.isDirectory())
                    throw new IllegalArgumentException("Destination file path error" + " " + destinationPath);
            }
            buildMediaSource(uri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void onVideoClicked() {
        try {
            if (isVideoEnded) {
                seekTo(lastMinValue);
                videoPlayer.setPlayWhenReady(true);
                return;
            }
            if ((currentDuration - lastMaxValue) > 0)
                seekTo(lastMinValue);
            videoPlayer.setPlayWhenReady(!videoPlayer.getPlayWhenReady());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seekTo(long sec) {
        if (videoPlayer != null)
            videoPlayer.seekTo(sec * 1000);
    }

    private void buildMediaSource(Uri mUri) {
        try {
            DataSource.Factory dataSourceFactory=new DefaultDataSourceFactory(this,getString(R.string.app_name));
            MediaSource mediaSource=new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mUri));
            videoPlayer.addMediaSource(mediaSource);
            videoPlayer.prepare();
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.addListener(new Player.EventListener() {
                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    imagePlayPause.setVisibility(playWhenReady ? View.GONE :
                            View.VISIBLE);
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    switch (state) {
                        case Player.STATE_ENDED:
                            LogMessage.v("onPlayerStateChanged: Video ended.");
                            imagePlayPause.setVisibility(View.VISIBLE);
                            isVideoEnded = true;
                            break;
                        case Player.STATE_READY:
                            isVideoEnded = false;
                            startProgress();
                            LogMessage.v("onPlayerStateChanged: Ready to play.");
                            break;
                        default:
                            break;
                        case Player.STATE_BUFFERING:
                            LogMessage.v("onPlayerStateChanged: STATE_BUFFERING.");
                            break;
                        case Player.STATE_IDLE:
                            LogMessage.v("onPlayerStateChanged: STATE_IDLE.");
                            break;
                    }
                }

            });
            setImageBitmaps();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setImageBitmaps() {
        try {
            long diff = totalDuration / 8;
            int sec = 1;
            for (ImageView img : imageViews) {
                long interval = (diff * sec)*1000000;
                RequestOptions options = new RequestOptions().frame(interval);
                Glide.with(this)
                        .load(getIntent().getStringExtra(TrimVideo.TRIM_VIDEO_URI))
                        .apply(options)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(img);
                if (sec<totalDuration)
                    sec++;
            }
            seekbar.setVisibility(View.VISIBLE);
            txtStartDuration.setVisibility(View.VISIBLE);
            txtEndDuration.setVisibility(View.VISIBLE);

            seekbarController.setMaxValue(totalDuration).apply();
            seekbar.setMaxValue(totalDuration).apply();
            seekbar.setMaxStartValue((float) totalDuration).apply();
            if (trimType == 1) {
                seekbar.setFixGap(fixedGap).apply();
                lastMaxValue = totalDuration;
            } else if (trimType == 2) {
                seekbar.setMaxStartValue((float) minGap);
                seekbar.setGap(minGap).apply();
                lastMaxValue = totalDuration;
            } else if (trimType == 3) {
                seekbar.setMaxStartValue((float) maxToGap);
                seekbar.setGap(minFromGap).apply();
                lastMaxValue = maxToGap;
            } else {
                seekbar.setGap(2).apply();
                lastMaxValue = totalDuration;
            }
            if (hidePlayerSeek)
                seekbarController.setVisibility(View.GONE);

            seekbar.setOnRangeSeekbarFinalValueListener((minValue, maxValue) -> {
                if (!hidePlayerSeek)
                    seekbarController.setVisibility(View.VISIBLE);
            });

            seekbar.setOnRangeSeekbarChangeListener((minValue, maxValue) -> {
                long minVal = (long) minValue;
                long maxVal = (long) maxValue;
                if (lastMinValue != minVal) {
                    seekTo((long) minValue);
                    if (!hidePlayerSeek)
                        seekbarController.setVisibility(View.INVISIBLE);
                }
                lastMinValue = minVal;
                lastMaxValue = maxVal;
                LogMessage.e("lastMinValue " + lastMinValue + " lastMaxValue " + lastMaxValue);
                txtStartDuration.setText(TrimmerUtils.formatSeconds(minVal));
                txtEndDuration.setText(TrimmerUtils.formatSeconds(maxVal));
                if (trimType == 3)
                    setDoneColor(minVal, maxVal);
            });

            seekbarController.setOnSeekbarFinalValueListener(value -> {
                long value1 = (long) value;
                if (value1 < lastMaxValue && value1 > lastMinValue) {
                    seekTo(value1);
                    return;
                }
                if (value1 > lastMaxValue)
                    seekbarController.setMinStartValue((int) lastMaxValue).apply();
                else if (value1 < lastMinValue) {
                    seekbarController.setMinStartValue((int) lastMinValue).apply();
                    if (videoPlayer.getPlayWhenReady())
                        seekTo(lastMinValue);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDoneColor(long minVal, long maxVal) {
        try {
            if (menuDone == null)
                return;
            if ((maxVal - minVal) <= maxToGap) {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhite)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = true;
            } else {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhiteLt)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PER_REQ_CODE) {
            if (isPermissionOk(grantResults))
                setDataInView();
            else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayer != null)
            videoPlayer.release();
        if (progressView != null && progressView.isShowing())
            progressView.dismiss();
        stopRepeatingTask();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuDone = menu.findItem(R.id.action_done);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            if (SystemClock.elapsedRealtime() - lastClickedTime < 800)
                return true;
            lastClickedTime = SystemClock.elapsedRealtime();
            validateVideo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void validateVideo() {
        if (isValidVideo) {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "";
            if (destinationPath != null)
                path = destinationPath;
            int fileNo = 0;
            String fName = "trimmed_video_";
            File newFile = new File(path + File.separator +
                    (fName) + "." + TrimmerUtils.getFileExtension(this, uri));
            while (newFile.exists()) {
                fileNo++;
                newFile = new File(path + File.separator +
                        (fName + fileNo) + "." + TrimmerUtils.getFileExtension(this, uri));
            }
            outputPath = String.valueOf(newFile);
            LogMessage.v("outputPath::" + outputPath);
            LogMessage.v("sourcePath::" + uri);
            videoPlayer.setPlayWhenReady(false);
            showProcessingDialog();
            String[] complexCommand;
            if (compressOption != null)
                complexCommand=getCompressionCommand();
            else if (isAccurateCut)
                complexCommand = getAccurateBinary();
            else {
                complexCommand = new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                        "-i", String.valueOf(uri),
                        "-t",
                        TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue),
                        "-async", "1", "-strict", "-2","-c","copy", outputPath};
            }
            execFFmpegBinary(complexCommand, true);
        } else
            Toast.makeText(this, getString(R.string.txt_smaller) + " " + TrimmerUtils.getLimitedTimeFormatted(maxToGap), Toast.LENGTH_SHORT).show();
    }

    private String[] getCompressionCommand() {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(String.valueOf(uri));
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        int w = TrimmerUtils.clearNull(width).isEmpty() ? 0 : Integer.parseInt(width);
        int h = Integer.parseInt(height);

        //Default compression option
        if (compressOption.getWidth()!=0 || compressOption.getHeight()!=0
                || !compressOption.getBitRate().equals("0k")){
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                    "-i", String.valueOf(uri), "-s", compressOption.getWidth() + "x" +
                    compressOption.getHeight(),
                    "-r", String.valueOf(compressOption.getFrameRate()),
                    "-vcodec", "mpeg4", "-b:v",
                    compressOption.getBitRate(), "-b:a", "48000", "-ac", "2", "-ar",
                    "22050","-t",
                    TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        }
        //Dividing high resolution video by 2(ex: taken with camera)
        else if (w >= 800) {
            w = w / 2;
            h = Integer.parseInt(height) / 2;
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                    "-i", String.valueOf(uri),
                    "-s", w + "x" + h, "-r", "30",
                    "-vcodec", "mpeg4", "-b:v",
                    "1M", "-b:a", "48000", "-ac", "2", "-ar", "22050",
                    "-t",
                    TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        } else {
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                    "-i", String.valueOf(uri), "-s", w + "x" + h, "-r",
                    "30", "-vcodec", "mpeg4", "-b:v",
                    "400K", "-b:a", "48000", "-ac", "2", "-ar", "22050",
                    "-t",
                    TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        }
    }

    private void execFFmpegBinary(final String[] command, boolean retry) {
        try {
            FFmpeg.executeAsync(command, (executionId1, returnCode) -> {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    dialog.dismiss();
                    Intent intent = new Intent();
                    intent.putExtra(TrimVideo.TRIMMED_VIDEO_PATH, outputPath);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    if (dialog.isShowing())
                        dialog.dismiss();
                } else {
                    if (retry && !isAccurateCut && compressOption==null) {
                        File newFile = new File(outputPath);
                        if (newFile.exists())
                            newFile.delete();
                        execFFmpegBinary(getAccurateBinary(), false);
                    } else {
                        if (dialog.isShowing())
                            dialog.dismiss();
                        runOnUiThread(() ->
                                Toast.makeText(ActVideoTrimmer.this, "Failed to trim", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getAccurateBinary() {
        return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue)
                , "-i", String.valueOf(uri), "-t",
                TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue),
                "-async", "1", outputPath};
    }

    private void showProcessingDialog() {
        try {
            dialog = new Dialog(this);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.alert_convert);
            TextView txtCancel = dialog.findViewById(R.id.txt_cancel);
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            txtCancel.setOnClickListener(v -> {
                dialog.dismiss();
                FFmpeg.cancel();
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION);
        } else
            return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

    }

    private boolean checkPermission(String... permissions) {
        boolean allPermitted = false;
        for (String permission : permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
            if (!allPermitted)
                break;
        }
        if (allPermitted)
            return true;
        ActivityCompat.requestPermissions(this, permissions,
                PER_REQ_CODE);
        return false;
    }


    private boolean isPermissionOk(int... results) {
        boolean isAllGranted = true;
        for (int result : results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false;
                break;
            }
        }
        return isAllGranted;
    }

    void startProgress() {
        updateSeekbar.run();
    }

    void stopRepeatingTask() {
        seekHandler.removeCallbacks(updateSeekbar);
    }

    Runnable updateSeekbar = new Runnable() {
        @Override
        public void run() {
            try {
                currentDuration = videoPlayer.getCurrentPosition() / 1000;
                if (!videoPlayer.getPlayWhenReady())
                    return;
                if (currentDuration <= lastMaxValue)
                    seekbarController.setMinStartValue((int) currentDuration).apply();
                else
                    videoPlayer.setPlayWhenReady(false);
            } finally {
                seekHandler.postDelayed(updateSeekbar, 1000);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AUDIO_REQUEST){
            if(resultCode == RESULT_OK) {
              //  showProcessingDialog();
                Uri audioUri = data.getData();
                Log.e(TAG," audioUri " + audioUri);
                try {
                    String uriString = audioUri.toString();
                    File myFile = new File(uriString);
                    audioPath = getPath(ActVideoTrimmer.this,audioUri);
                    File f = new File(audioPath);
                    long fileSizeInBytes = f.length();
                    long fileSizeInKB = fileSizeInBytes / 1024;
                    long fileSizeInMB = fileSizeInKB / 1024;
                    if (fileSizeInMB > 8) {
                        Toast.makeText(this, "Sorry file size is large", Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG,"audiopath " + audioPath + "f " + f.getAbsolutePath());
                    Log.e(TAG,"appDirectoryPath " + appDirectoryPath);
                    File output = new File(appDirectoryPath,OUTPUT_FILE_NAME+ ".mp4");

                    if(output.exists()){
                        number++;
                        output = new File(appDirectoryPath,OUTPUT_FILE_NAME + "_" +
                                number + ".mp4");
                    }

                    outputPath = output.getAbsolutePath();
                    Log.e(TAG,"outputPath " + outputPath);
                    Log.e(TAG,"uri.getPath() " + uri.getPath());
                    // lastMaxValue - lastMinValue
                    Log.e(TAG,"lastMinValue " + lastMinValue + " lastMaxValue " + lastMaxValue);
                    String[] command = {"-i", uri.getPath(), "-i", audioPath, "-filter_complex",
                            "[0:a]atrim=end=" + lastMinValue + ",asetpts=PTS-STARTPTS[aud1];" +
                            "[1:a]atrim=" + lastMinValue + ":" + lastMaxValue + ",asetpts=PTS-STARTPTS[aud2];"+
                            "[0:a]atrim=start=" + lastMaxValue + ",asetpts=PTS-STARTPTS[aud3];" +
                            "[aud1][aud2][aud3]concat=n=3:v=0:a=1[aout]",
                            "-map", "0:v", "-map", "[aout]", "-c:v", "copy", "-c:a","libfdk_aac", outputPath};
//                    String[] command  = {"-i", uri.getPath(), "-i", audioPath, "-c:v", "copy",
//                            "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest",
//                            output.getAbsolutePath()};
                    execAddAudioFFmpegBinary(command);
                } catch (Exception e) {
                    //handle exception
                    Toast.makeText(ActVideoTrimmer.this, "Unable to process,try again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

//    private void execAddAudioFFmpegBinary(final String[] command, boolean retry) {
//        try {
//            FFmpeg.executeAsync(command, (executionId1, returnCode) -> {
//                if (returnCode == RETURN_CODE_SUCCESS) {
//                    Log.e(TAG,"command successful audio added");
//                    dialog.dismiss();
//                    File outputPath = new File(appDirectoryPath,OUTPUT_FILE_NAME);
//                    Log.e(TAG,"outputPath " + outputPath.getAbsoluteFile());
//                    uri = Uri.fromFile(outputPath);
//                    setDataInView();
//                    seekTo(0);
////                    Intent intent = new Intent();
////                    intent.putExtra(TrimVideo.TRIMMED_VIDEO_PATH, outputPath);
////                    setResult(RESULT_OK, intent);
////                    finish();
//                } else if (returnCode == RETURN_CODE_CANCEL) {
//                    if (dialog.isShowing())
//                        dialog.dismiss();
//                } else {
//                    if (retry && !isAccurateCut && compressOption==null) {
//                        File newFile = new File(outputPath);
//                        if (newFile.exists())
//                            newFile.delete();
//                        execAddAudioFFmpegBinary(getAccurateBinary(), false);
//                    } else {
//                        if (dialog.isShowing())
//                            dialog.dismiss();
//                        runOnUiThread(() ->
//                                Toast.makeText(ActVideoTrimmer.this, "Failed to trim", Toast.LENGTH_SHORT).show());
//                    }
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void execAddAudioFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.e(TAG, "FAILED with output : " + s);
                    Toast.makeText(ActVideoTrimmer.this, s, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onSuccess(String s) {
                    Log.e(TAG, "SUCCESS with output : " + s);
                    dialog.dismiss();

                }
                @Override
                public void onProgress(String s) {
                    Log.e(TAG, "progress : " + s);
                    //  spinnerVideoDetails.setVisibility(View.VISIBLE);
                }

                @Override
                public void onStart() {
                    Log.e(TAG, "Started command : ffmpeg " + command);
                    showProcessingDialog();
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "Finished command : ffmpeg " + command);
                    dialog.dismiss();
                    File outputPath = new File(appDirectoryPath,OUTPUT_FILE_NAME);
                    Log.e(TAG,"outputPath " + outputPath.getAbsoluteFile());
                    uri = Uri.fromFile(outputPath);
                    setDataInView();
                    seekTo(0);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG,"FFmpegCommandAlreadyRunningException " + e.getMessage());
        }
    }
}
