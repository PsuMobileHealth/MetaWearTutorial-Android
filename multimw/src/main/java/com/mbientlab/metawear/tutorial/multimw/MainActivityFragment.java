/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.tutorial.multimw;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Switch;

import java.util.HashMap;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements ServiceConnection {
    private final Handler taskScheduler;
    private final HashMap<DeviceState, MetaWearBoard> stateToBoards;
    private MetaWearBleService.LocalBinder binder;

    private ConnectedDevicesAdapter connectedDevices= null;

    public MainActivityFragment() {
        stateToBoards = new HashMap<>();
        taskScheduler= new Handler();
    }

    public void addNewDevice(BluetoothDevice btDevice) {
        final DeviceState newDeviceState= new DeviceState(btDevice);
        final MetaWearBoard newBoard= binder.getMetaWearBoard(btDevice);

        stateToBoards.put(newDeviceState, newBoard);
        newBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                connectedDevices.add(newDeviceState);

                try {
                    newBoard.getModule(Switch.class).routeData().fromSensor().stream("switch_stream").commit()
                            .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("switch_stream", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            newDeviceState.pressed = msg.getData(Boolean.class);
                                            connectedDevices.notifyDataSetChanged();
                                        }
                                    });
                                }
                            });
                    final Accelerometer accelModule= newBoard.getModule(Accelerometer.class);
                    accelModule.routeData().fromOrientation().stream("orientation_stream").commit()
                            .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("orientation_stream", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            newDeviceState.deviceOrientation = msg.getData(Accelerometer.BoardOrientation.class).toString();
                                        }
                                    });
                                    accelModule.enableOrientationDetection();
                                    accelModule.start();
                                }
                            });
                } catch (UnsupportedModuleException e) {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void disconnected() {
                connectedDevices.remove(newDeviceState);
            }

            @Override
            public void failure(int status, Throwable error) {
                connectedDevices.remove(newDeviceState);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        connectedDevices= new ConnectedDevicesAdapter(getActivity(), R.id.metawear_status_layout);
        connectedDevices.setNotifyOnChange(true);
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ListView connectedDevicesView= (ListView) view.findViewById(R.id.connected_devices);
        connectedDevicesView.setAdapter(connectedDevices);
        connectedDevicesView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                DeviceState current= connectedDevices.getItem(position);
                final MetaWearBoard selectedBoard= stateToBoards.get(current);

                try {
                    Accelerometer accelModule = selectedBoard.getModule(Accelerometer.class);
                    accelModule.stop();
                    accelModule.disableOrientationDetection();

                    selectedBoard.removeRoutes();
                    selectedBoard.getModule(Debug.class).disconnect();
                } catch (UnsupportedModuleException e) {
                    // Not a big deal if the try catch fails
                    Log.w("multimw", e);

                    taskScheduler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            selectedBoard.disconnect();
                        }
                    }, 100);
                }

                connectedDevices.remove(current);
                return false;
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (MetaWearBleService.LocalBinder) service;
        binder.executeOnUiThread();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
