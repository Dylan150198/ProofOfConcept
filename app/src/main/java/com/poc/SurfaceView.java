package com.poc;

import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtsp.RtspCamera1;
import com.pedro.rtsp.rtsp.Protocol;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SurfaceView extends AppCompatActivity implements ConnectCheckerRtsp, View.OnClickListener, SurfaceHolder.Callback {
    private RtspCamera1 rtspCamera1;
    private Button button;
    private Button bRecord;
    private EditText etUrl;
    private String currentDateAndTime = "";
    private File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/rtmp-rtsp-stream-client-java");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_surface_view);
        android.view.SurfaceView surfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.b_start_stop);
        button.setOnClickListener(this);
        bRecord = findViewById(R.id.b_record);
        bRecord.setOnClickListener(this);
        Button switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
        etUrl = findViewById(R.id.et_rtp_url);
        etUrl.setHint(R.string.hint_rtsp);
        rtspCamera1 = new RtspCamera1(surfaceView, this);
        rtspCamera1.setProtocol(Protocol.UDP);
        rtspCamera1.setReTries(10);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        rtspCamera1.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtspCamera1.isRecording()) {
            rtspCamera1.stopRecord();
            bRecord.setText(R.string.start_record);
            Toast.makeText(this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
        if (rtspCamera1.isStreaming()) {
            rtspCamera1.stopStream();
            button.setText(getResources().getString(R.string.start_button));
        }
        rtspCamera1.stopPreview();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_start_stop:
                if (!rtspCamera1.isStreaming()) {
                    if (rtspCamera1.isRecording()
                            || rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
                        button.setText(R.string.stop_button);
                        rtspCamera1.startStream(etUrl.getText().toString());
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    button.setText(R.string.start_button);
                    rtspCamera1.stopStream();
                }
                break;
            case R.id.switch_camera:
                try {
                    rtspCamera1.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.b_record:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (!rtspCamera1.isRecording()) {
                        try {
                            if (!folder.exists()) {
                                folder.mkdir();
                            }
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                            currentDateAndTime = sdf.format(new Date());
                            if (!rtspCamera1.isStreaming()) {
                                if (rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
                                    rtspCamera1.startRecord(
                                            folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                                    bRecord.setText(R.string.stop_record);
                                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Error preparing stream, This device cant do it",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                rtspCamera1.startRecord(
                                        folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                                bRecord.setText(R.string.stop_record);
                                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            rtspCamera1.stopRecord();
                            bRecord.setText(R.string.start_record);
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        rtspCamera1.stopRecord();
                        bRecord.setText(R.string.start_record);
                        Toast.makeText(this,
                                "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionSuccessRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SurfaceView.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtsp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rtspCamera1.shouldRetry(reason)) {
                    Toast.makeText(SurfaceView.this, "Retry", Toast.LENGTH_SHORT)
                            .show();
                    rtspCamera1.reTry(5000);  //Wait 5s and retry connect stream
                } else {
                    Toast.makeText(SurfaceView.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                            .show();
                    rtspCamera1.stopStream();
                    button.setText(R.string.start_button);
                }
            }
        });
    }

    @Override
    public void onDisconnectRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SurfaceView.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SurfaceView.this, "Auth error", Toast.LENGTH_SHORT).show();
                rtspCamera1.stopStream();
                button.setText(R.string.start_button);
            }
        });
    }

    @Override
    public void onAuthSuccessRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SurfaceView.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
