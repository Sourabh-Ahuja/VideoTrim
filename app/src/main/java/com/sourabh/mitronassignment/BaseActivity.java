package com.sourabh.mitronassignment;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    public FFmpeg ffmpeg;
    public boolean isFfMPegSupported;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadFfMPEGBinaries();
    }

    private void loadFfMPEGBinaries() {
        try {
            if (ffmpeg == null) {
                Log.d(TAG, "ffmpeg : null");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    isFfMPegSupported = true;
                    Log.d(TAG, "ffmpeg : correct Loaded " + isFfMPegSupported);
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            Log.d(TAG, "EXception not supported : " + e);
        }
    }

    private void showUnsupportedExceptionDialog() {
        Toast.makeText(this, "FfMPeg not supported", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(ffmpeg != null) {
            ffmpeg.killRunningProcesses();
            ffmpeg = null;
        }
    }
}
