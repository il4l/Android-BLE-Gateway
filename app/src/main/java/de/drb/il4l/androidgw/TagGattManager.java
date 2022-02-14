/*
 * Copyright (c) 2022, Dr. Brunthaler GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.drb.il4l.androidgw;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import de.drb.il4l.androidgw.profile.callback.RangingDataCallback;
import de.drb.il4l.androidgw.profile.callback.VersionDataCallback;
import de.drb.il4l.androidgw.viewmodels.GattConnection;

import static no.nordicsemi.android.ble.ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * This class implements the ranging profile for DWM1001 tags. For each connection there will be
 * one instance of this class
 */
public class TagGattManager extends BleManager {

    private static final String TAG = TagGattManager.class.getSimpleName();

    public static final UUID RANGING_SERVICE = UUID.fromString("8FA50001-BCC2-4BE4-B49E-4F5C34546F6C");
    public static final UUID RANGING_CHAR = UUID.fromString("8FA50006-BCC2-4BE4-B49E-4F5C34546F6C");
    public static final UUID RANGE_CHAR = UUID.fromString("8FA50002-BCC2-4BE4-B49E-4F5C34546F6C");
    public static final UUID VERSION_CHAR  = UUID.fromString("8FA50003-BCC2-4BE4-B49E-4F5C34546F6C");

    //public static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //public static final UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    //public static final UUID BATTERY_LEVEL = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mRangeCharacteristic, mRange2Characteristic;
    private BluetoothGattCharacteristic mVersionCharacteristic;
    private BluetoothGattCharacteristic mRangingCharacteristic;
    private BluetoothGattCharacteristic mBatLevel;

    private final GattConnection mConnection;
    private boolean mAutoReconnect = true;
    private boolean mSupported = false;
    private long mLastUpdateTime;

    public TagGattManager(@NonNull final Context context, GattConnection gattConnection) {
        super(context);

        mLastUpdateTime = System.currentTimeMillis();
        mConnection = gattConnection;
    }

    public boolean isAutoReconnect() {
        return mAutoReconnect;
    }

    public void setAutoReconnect(boolean reconnect) {
        mAutoReconnect = reconnect;
    }

    public GattConnection getConnection() {
        return mConnection;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    @Override
    protected boolean shouldClearCacheWhenDisconnected() {
        return !mSupported;
    }

    private	final RangingDataCallback mRangingCallback = new RangingDataCallback() {
        @Override
        public void onRangingChanged(@NonNull final BluetoothDevice device,
                                   final Data data) {
            refreshUpdateRate();

            try {
                GatewayService service = ((BLE_GW_Application)getContext().getApplicationContext()).getGatewayService();
                service.getMqtt().publishRanging(getConnection(), data);

                service.deviceNotifyUpdate(getConnection());
            } catch (MqttException e) {
                Log.w(TAG, "Failed to publish ranging data to MQTT-Broker", e);
            }

            if (BLE_GW_Application.showPOS) {
                String s = "";
                String[] arr;
                try {
                    arr = data.getStringValue(0).split(",");
                    if (arr.length >= 3){
                        for (int i=0; i<3; i++){
                            if (i==0) s+="x:"+arr[i];
                            if (i==1) s+=" y:"+arr[i];
                            if (i==2) s+=" z:"+arr[i];

                        }
                    }
                } catch (NullPointerException ignored){} //in case tag data is corrupted

                BLE_GW_Application.debugPos = s;
            }
        }

        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {
            try {
                log(Log.WARN, "Invalid data received: " + data);
            } catch (NullPointerException e) {
                log(Log.WARN, "No data received");
            }
        }
    };

    private	final VersionDataCallback mVersionCallback = new VersionDataCallback() {
        @Override
        public void onVersionReceived(int liteID, final int mainV, final int subV) {
            Log.i(TAG,"--> liteID '"+liteID+"': "+mainV+"."+subV);
        }

        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {
            log(Log.WARN, "Invalid data received: " + data);
        }

    };

    /**
     * BluetoothGatt callbacks object.f
     */
    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
        @Override
        protected void initialize() {
        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {

            final BluetoothGattService ranging_service = gatt.getService(RANGING_SERVICE);
            if (ranging_service != null) {
                mRangeCharacteristic = ranging_service.getCharacteristic(RANGE_CHAR);
                mRangingCharacteristic = ranging_service.getCharacteristic(RANGING_CHAR);
                mVersionCharacteristic = ranging_service.getCharacteristic(VERSION_CHAR);
            }
            mSupported = mRangingCharacteristic != null;
            Log.i(TAG, "Checking if the services are supported: "+mSupported);

            return mSupported;
        }

        @Override
        protected void onDeviceDisconnected() {
            mBatLevel = null;
        }
    };

    /**
     * Enable bluetooth ATT notifications
     */
    public void enableATTNotifications() {
        Log.d(TAG, "Enabling ATT notifications for " + getConnection().getServerAddr());
        setNotificationCallback(mRangingCharacteristic).with(mRangingCallback);
        enableNotifications(mRangingCharacteristic).enqueue();
        requestVersion();
    }

    /**
     * Read version ATT
     */
    public void requestVersion( ){
        readCharacteristic(mVersionCharacteristic).with(mVersionCallback).enqueue();
        Log.i(TAG,"enqueued Version read");
    }

    /**
     * Set BLE connection priority and MTU
     */
    public void setConnPriority() {
        requestConnectionPriority(CONNECTION_PRIORITY_HIGH).enqueue();
        requestMtu(240).enqueue();
    }

    /**
     * Refresh connection update rate
     */
    private void refreshUpdateRate() {
        long now = System.currentTimeMillis();

        long delta = now - mLastUpdateTime;
        float updateRate = 1000.0f / delta;
        mLastUpdateTime = now;

        getConnection().setURate(updateRate);
    }
}
