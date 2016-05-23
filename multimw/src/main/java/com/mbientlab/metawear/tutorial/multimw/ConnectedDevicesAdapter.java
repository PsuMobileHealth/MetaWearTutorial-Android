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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Created by etsai on 5/22/2016.
 */
public class ConnectedDevicesAdapter extends ArrayAdapter<DeviceState> {
    public ConnectedDevicesAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView= LayoutInflater.from(getContext()).inflate(R.layout.metawear_status, parent, false);

            viewHolder= new ViewHolder();
            viewHolder.deviceName= (TextView) convertView.findViewById(R.id.status_device_name);
            viewHolder.deviceAddress= (TextView) convertView.findViewById(R.id.status_mac_address);
            viewHolder.deviceOrientation= (TextView) convertView.findViewById(R.id.status_orientation);
            viewHolder.switchState= (RadioGroup) convertView.findViewById(R.id.status_button);
            viewHolder.connectingText= (TextView) convertView.findViewById(R.id.text_connecting);
            viewHolder.connectingProgress= (ProgressBar) convertView.findViewById(R.id.connecting_progress);

            convertView.setTag(viewHolder);
        } else {
            viewHolder= (ViewHolder) convertView.getTag();
        }

        DeviceState state= getItem(position);
        final String deviceName= state.btDevice.getName();

        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText(R.string.label_unknown_device);
        }
        viewHolder.deviceAddress.setText(state.btDevice.getAddress());

        if (state.connecting) {
            viewHolder.connectingProgress.setVisibility(View.VISIBLE);
            viewHolder.connectingText.setVisibility(View.VISIBLE);
            viewHolder.deviceOrientation.setVisibility(View.GONE);
            viewHolder.switchState.setVisibility(View.GONE);
        } else {
            viewHolder.deviceOrientation.setVisibility(View.VISIBLE);
            viewHolder.switchState.setVisibility(View.VISIBLE);

            if (state.deviceOrientation != null) {
                viewHolder.deviceOrientation.setText(state.deviceOrientation);
            }

            if (state.pressed) {
                viewHolder.switchState.check(R.id.switch_radio_pressed);
                convertView.findViewById(R.id.switch_radio_pressed).setEnabled(true);
                convertView.findViewById(R.id.switch_radio_released).setEnabled(false);
            } else {
                viewHolder.switchState.check(R.id.switch_radio_released);
                convertView.findViewById(R.id.switch_radio_released).setEnabled(true);
                convertView.findViewById(R.id.switch_radio_pressed).setEnabled(false);
            }

            viewHolder.connectingProgress.setVisibility(View.GONE);
            viewHolder.connectingText.setVisibility(View.GONE);
        }

        return convertView;
    }

    private class ViewHolder {
        public TextView deviceName, deviceAddress, deviceOrientation, connectingText;
        public RadioGroup switchState;
        public ProgressBar connectingProgress;
    }

    public void update(DeviceState newState) {
        int pos= getPosition(newState);
        if (pos == -1) {
            add(newState);
        } else {
            DeviceState current= getItem(pos);
            current.pressed= newState.pressed;
            current.deviceOrientation= newState.deviceOrientation;
            notifyDataSetChanged();
        }
    }
}
