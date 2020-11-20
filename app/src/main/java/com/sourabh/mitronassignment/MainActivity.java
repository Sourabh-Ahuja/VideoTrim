package com.sourabh.mitronassignment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.gowtham.library.utils.TrimVideo;
import static com.sourabh.mitronassignment.utils.Constants.PATH;
import static com.sourabh.mitronassignment.utils.Constants.VIDEO_URI;
import static com.sourabh.mitronassignment.utils.Constants.getRootDirPath;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_VIDEO_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button select_video = findViewById(R.id.select_video);
        select_video.setOnClickListener(v -> {
            requestPermission();
        });
    }

    private void requestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission();
            } else {
                requestVideoFromGallery();
            }
        }
    }

    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
    }

    private boolean checkIfAlreadyHavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //granted
                requestVideoFromGallery();
            } else {
                //not granted
                Toast.makeText(this, "Need Permission to do Task", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void requestVideoFromGallery() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_VIDEO_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // requesting video from gallery and trim it
            if (requestCode == REQUEST_VIDEO_CODE && data != null) {

                if (resultCode == RESULT_OK) {
                    Uri selectedImageUri = data.getData();
                    Log.e(TAG,"selectedImageUri " + selectedImageUri);
                    String appDirectoryPath = getRootDirPath(MainActivity.this);
                    Log.e(TAG,"appDirectoryPath " + appDirectoryPath);
                    TrimVideo.activity(String.valueOf(selectedImageUri))
                            .setDestination("/storage/emulated/0/DOWNLOADS")
                            .start(this);
                }

              // playing trimmed video in video activity
        } if (requestCode == TrimVideo.VIDEO_TRIMMER_REQ_CODE && data != null) {

                Uri uri = Uri.parse(TrimVideo.getTrimmedVideoPath(data));
                Log.e(TAG,"Trimmed path:: "+uri);
                String selectedVideoPath = uri.getPath();
                Log.e(TAG,"selectedVideoPath " + selectedVideoPath);
                if (selectedVideoPath != null) {
                    Intent intent = new Intent(MainActivity.this,
                            VideoActivity.class);
                    intent.putExtra(PATH, selectedVideoPath);
                    intent.putExtra(VIDEO_URI, uri.toString());
                    startActivity(intent);
                }

        }
    }
}