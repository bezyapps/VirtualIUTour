package com.rea.learn;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;

import boofcv.android.gui.CameraPreview;
import boofcv.android.gui.VideoDisplayActivity;


public class MainActivity extends VideoDisplayActivity {


    boolean change = false;
    View augmentView;
    Button markerButton;
    ListView listViewClassSchedules;
    ScheduleAdapter scheduleAdapter;
    Vector<Schedule> schedules;

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setShowFPS(true);
        Intent intent = getIntent();
        if (intent.hasExtra(Settings.IP_ADDRESS) && intent.hasExtra(Settings.SKIP_RATE)) {
            setProcessing(new BitmapVariableHistoGramProcessing(this, s.width, s.height, intent.getStringExtra(Settings.IP_ADDRESS).trim(),
                    intent.getIntExtra(Settings.SKIP_RATE, 7)));
        } else {
            setProcessing(new BitmapVariableHistoGramProcessing(this, s.width, s.height));
        }
    }


    private synchronized void initAugmentView(MainActivity mainActivity) {
        LayoutInflater layoutInflater = LayoutInflater.from(mainActivity);
        augmentView = layoutInflater.inflate(R.layout.augment, null);
        markerButton = (Button) augmentView.findViewById(R.id.buttonMarker);
        listViewClassSchedules = (ListView) augmentView.findViewById(R.id.listViewClassSchedules);
        schedules = new Vector<>();
        scheduleAdapter = new ScheduleAdapter(mainActivity, R.layout.augment_list_item, schedules);
        listViewClassSchedules.setAdapter(scheduleAdapter);
        mainActivity.addContentView(augmentView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        augmentView.bringToFront();
        markerButton.setGravity(Gravity.LEFT);
        markerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listViewClassSchedules.getVisibility() == View.GONE) {
                    listViewClassSchedules.setVisibility(View.VISIBLE);
                } else {
                    listViewClassSchedules.setVisibility(View.GONE);
                }
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAugmentView(this);
    }

    Camera.Size s;

    @Override
    protected Camera openConfigureCamera(Camera.CameraInfo cameraInfo) {
        Camera mCamera = selectAndOpenCamera(cameraInfo);
        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Camera.Size s = sizes.get(closest(sizes, 320, 240));
        this.s = s;
        param.setPreviewSize(s.width, s.height);
        Log.e("ERBL", s.width + " , " + s.height);
        mCamera.setParameters(param);
        return mCamera;
    }

    private Camera selectAndOpenCamera(Camera.CameraInfo cameraInfo) {
        int numberOfCameras = Camera.getNumberOfCameras();
        int selected = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                selected = i;
                break;
            } else {
                // default to a front facing camera if a back facing one can't be found
                selected = i;
            }
        }

        if (selected == -1) {
            dialogNoCamera();
            return null; // won't ever be called
        } else {
            return Camera.open(selected);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int test = item.getItemId();
        String title = item.getTitle().toString();
        Log.e("TEST ", test + title);
        switch (item.getItemId()) {
            case R.id.action_one_object:
                Toast.makeText(this, "TEST", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_two_object:
                Toast.makeText(this, "TEST2", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_csv:
                Toast.makeText(this, "TEST3", Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }

    private void dialogNoCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device has no cameras!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Goes through the size list and selects the one which is the closest specified size
     */
    public static int closest(List<Camera.Size> sizes, int width, int height) {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size s = sizes.get(i);

            int dx = s.width - width;
            int dy = s.height - height;

            int score = dx * dx + dy * dy;
            if (score < bestScore) {
                best = i;
                bestScore = score;
            }
        }

        return best;
    }
}
