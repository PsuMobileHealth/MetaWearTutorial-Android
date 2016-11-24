/*
 * Code taken from http://www.edumobile.org/android/touch-rotate-example-in-android/
 */
package com.mbientlab.tutorial.sensorfusion;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.SensorFusion;
import com.mbientlab.metawear.module.SensorFusion.AccRange;
import com.mbientlab.metawear.module.SensorFusion.GyroRange;
import com.mbientlab.metawear.module.SensorFusion.Mode;
import com.mbientlab.metawear.module.SensorFusion.Quaternion;

public class CubeActivity extends AppCompatActivity implements ServiceConnection {
    public final static String EXTRA_BT_DEVICE= "com.mbientlab.tutorial.sensorfusion.CubeActivity.EXTRA_BT_DEVICE";

    private BluetoothDevice btDevice;
    private MetaWearBoard board;

    @Override
    public void onBackPressed() {
        board.getModule(SensorFusion.class).stop();
        board.getModule(Debug.class).disconnect();

        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new CubeSurfaceView(this);
        setContentView(mGLSurfaceView);
        mGLSurfaceView.requestFocus();
        mGLSurfaceView.setFocusableInTouchMode(true);

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    private final long FPS = (long) ((1 / 30f) * 1000L);
    private Handler taskScheduler = new Handler();
    private CubeSurfaceView mGLSurfaceView;
    private final Runnable updateScene = new Runnable() {
        @Override
        public void run() {
            mGLSurfaceView.requestRender();
            taskScheduler.postDelayed(updateScene, FPS);
        }
    };

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BtleService.LocalBinder binder = (BtleService.LocalBinder) service;
        board = binder.getMetaWearBoard(btDevice);

        SensorFusion sensorFusion = board.getModule(SensorFusion.class);
        sensorFusion.configure()
                .mode(Mode.NDOF)
                .accRange(AccRange.AR_2G)
                .gyroRange(GyroRange.GR_250DPS)
                .commit();
        sensorFusion.quaternion().addRoute(source -> source.stream((msg, env) -> mGLSurfaceView.updateRotation(msg.value(Quaternion.class))))
                .continueWith(ignored -> {
                    sensorFusion.quaternion().start();
                    sensorFusion.start();
                    return null;
                });
        taskScheduler.postDelayed(updateScene, FPS);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
