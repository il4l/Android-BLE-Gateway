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

import static de.drb.il4l.androidgw.BLE_GW_Application.CHANNEL_ID;
import static de.drb.il4l.androidgw.BLE_GW_Application.SEND_NOTIFY_DELAY;
import static de.drb.il4l.androidgw.viewmodels.GattConnection.CONNECTED;
import static de.drb.il4l.androidgw.viewmodels.GattConnection.CONNECTING;
import static de.drb.il4l.androidgw.viewmodels.GattConnection.DISCONNECTING;
import static de.drb.il4l.androidgw.viewmodels.GattConnection.DISCOVERED;
import static de.drb.il4l.androidgw.viewmodels.GattConnection.NOT_STARTED;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.DecimalFormat;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import de.drb.il4l.androidgw.mqtt.MqttForwarder;
import de.drb.il4l.androidgw.viewmodels.GattConnection;
import de.drb.il4l.androidgw.R;

/**
 * Foreground-Service that manages connection to BLE tags.
 *
 * @author  Lars Schymik
 * @author  Michael Pekar
 */
public class GatewayService extends Service implements BleManagerCallbacks {

    private final static String TAG = GatewayService.class.getSimpleName();
    private final static int ONGOING_NOTIFICATION_ID = 1;

    public final static String BROADCAST_CHANGED_STATE = "changedState";
    public final static String BROADCAST_MQTT_CONNECTION = "mqttConnection";

    private final ObservableMap<String, TagGattManager> mConnections = new ObservableArrayMap<>();
    private LocalBroadcastManager mLocalBroadcastManager;
    private MqttForwarder mMqtt;

    private boolean mCachedMqttIsConnected = false;
    private long mLastDeviceUpdateNotification = 0;

    // Mqtt event listener used to communicate connection state with other parts of the app.
    private MqttCallbackExtended mMqttCallback = new MqttCallbackExtended() {
        private static final String MqttTAG = "MQTT";

        @Override
        public void connectComplete(boolean b, String server) {
            Log.i(MqttTAG, "MQTT connected!" + server);
            broadcastMqttConnection(true);
            updateForegroundNotification();
        }

        @Override
        public void connectionLost(Throwable e) {
            Log.w(MqttTAG, "connection lost!", e);
            broadcastMqttConnection(false);
            updateForegroundNotification();
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            Log.d(MqttTAG, mqttMessage.toString());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }
    };

    private final IBinder mBinder = new GatewayService.LocalBinder();
    public class LocalBinder extends Binder {
        GatewayService getService() {
            return GatewayService.this;
        }
    }

    public GatewayService(){
        Log.d(TAG, "GateWay-Service created!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Get a unique id so we can distinguish different devices.
        String deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = String.format("%s %s %d (%s)",
                    Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT, Build.VERSION.RELEASE);
        }
        mMqtt = new MqttForwarder(getApplicationContext(), deviceId, mMqttCallback);
        try {
            mMqtt.connect();
        } catch (MqttException e) {
            Log.i(TAG, "Couldn't connect to MQTT-Broker!", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int r = super.onStartCommand(intent, flags, startId);

        // Look if there is any device saved for auto reconnect
        String device = Preferences.getPrefs("reconnectTag", getApplicationContext());
        if (!device.equals("notfound")){
            deviceConnect(new GattConnection(device, GattConnection.NOT_STARTED));
        }

        // Show service notification
        updateForegroundNotification();

        return r;
    }

    /**
     * Attempt to establish connection with device
     * @param connection                    target device
     */
    public void deviceConnect(GattConnection connection) {
        String deviceAddress = connection.getServerAddr();

        if (connection.getConnectionState() == CONNECTING ||
            connection.getConnectionState() == CONNECTED) {
            Log.i(TAG, "Ignoring deviceConnect(" + deviceAddress + ") because already connected");
            return;
        }

        TagGattManager tagConnection = mConnections.get(deviceAddress);
        if (tagConnection ==  null) {
            tagConnection = new TagGattManager(getApplicationContext(), connection);
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        Log.i(TAG, "deviceConnect(" + deviceAddress + ")");
        tagConnection.setGattCallbacks(this);
        tagConnection
                .connect(device)
                .retry(3, 50)
                .useAutoConnect(false)
                .enqueue();
        tagConnection.setConnPriority();
        changeConnState(connection, CONNECTING);
        mConnections.put(deviceAddress, tagConnection);
    }

    /**
     * Disconnect with device
     * @param connection                    target device
     * @throws IllegalArgumentException     if device is unknown
     */
    public void deviceDisconnect(GattConnection connection) {
        String deviceAddress = connection.getServerAddr();

        TagGattManager tagConnection = mConnections.get(deviceAddress);
        if (tagConnection ==  null) {
            throw new IllegalArgumentException("unknown device");
        }

        Log.i(TAG, "deviceDisconnect(" + deviceAddress + ")");
        tagConnection
                .disconnect()
                .enqueue();
        changeConnState(connection, DISCONNECTING);
        tagConnection.setAutoReconnect(false); // User issued disconnect, disable auto reconnect
    }

    /**
     * Broadcast that this device has new ranging information available
     * @param connection                    target device
     */
    public void deviceNotifyUpdate(GattConnection connection) {
        broadcastChangedState(connection.getServerAddr());

        // Throttle service notification updates
        long now = System.currentTimeMillis();
        if (now-mLastDeviceUpdateNotification > 5000) {
            updateForegroundNotification();
            mLastDeviceUpdateNotification = now;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "GW Service destroyed ...");
    }

    @Override
    public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
        broadcastChangedState(device.getAddress());
        Log.i(TAG, "App connecting to GATT Peripheral ...");
    }

    @Override
    public void onDeviceConnected(@NonNull final BluetoothDevice device) {
        Log.i(TAG, "Device connected: "+device.getAddress());
        TagGattManager tag = mConnections.get(device.getAddress());
        if (tag != null) {
            changeConnState(tag.getConnection(), CONNECTED);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(device.getAddress().getBytes());
            try {
                getMqtt().publish("ONLINE", msg);
            } catch (MqttException e) {
                Log.w(TAG, "Couldn't publish ONLINE message to MQTT-Broker", e);
            }
        }
    }

    @Override
    public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
        Log.i(TAG, "Device '" + device.getAddress() + "' is disconnecting from gateway..");

        broadcastChangedState(device.getAddress());

        MqttMessage msg = new MqttMessage();
        msg.setPayload(device.getAddress().getBytes());
        try {
            getMqtt().publish("OFFLINE", msg);
        } catch (MqttException e) {
            Log.w(TAG, "Couldn't publish OFFLINE message to MQTT-Broker", e);
        }
    }

    @Override
    public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {

        TagGattManager tag = mConnections.get(device.getAddress());
        if (tag == null) {
            Log.w(TAG, "Unknown device disconnected: " + device.getAddress());
            return;
        }

        tag.close();

        // Update connection state
        changeConnState(tag.getConnection(), NOT_STARTED);

        if (tag.isAutoReconnect()) {
            Log.i(TAG, "Connection to GATT Peripheral was closed unexpectedly! reconnecting..");

            changeConnState(tag.getConnection(), CONNECTING);
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

            tag.setGattCallbacks(this);
            tag
                    .connect(bluetoothDevice)
                    .retry(3, 50)
                    .useAutoConnect(false)
                    .enqueue();
            tag.setConnPriority();
        } else {
            mConnections.remove(device.getAddress());
            // Stop service when there are no connected tags
            if (mConnections.isEmpty()) {
                stopSelf();
            }

            broadcastChangedState(device.getAddress());
            Log.i(TAG, "Closed connection to GATT Peripheral...");
        }
    }

    @Override
    public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {
    }

    @Override
    public void onServicesDiscovered(@NonNull final BluetoothDevice device,
                                     final boolean optionalServicesFound) {
        Log.i(TAG,"Discovered services on GATT Peripheral");
        TagGattManager tag = mConnections.get(device.getAddress());
        if (tag != null) {
            changeConnState(tag.getConnection(), DISCOVERED);
        }
    }

    @Override
    public void onDeviceReady(@NonNull final BluetoothDevice device) {
        Log.i(TAG,"Activating notifications in "+ SEND_NOTIFY_DELAY +"ms");

        // Request bluetooth notifications after a short delay..
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                TagGattManager tagGattManager = mConnections.get(device.getAddress());
                if(tagGattManager != null) {
                    tagGattManager.enableATTNotifications();
                }
            }
        }, SEND_NOTIFY_DELAY);
    }

    @Override
    public void onBondingRequired(@NonNull final BluetoothDevice device) {
    }

    @Override
    public void onBonded(@NonNull final BluetoothDevice device) {
    }

    @Override
    public void onBondingFailed(@NonNull final BluetoothDevice device) {
    }

    @Override
    public void onError(@NonNull final BluetoothDevice device,
                        @NonNull final String message, final int errorCode) {
        Log.i(TAG,"BLE Device "+device.getAddress()+" reported an error: "+errorCode);
    }

    @Override
    public void onDeviceNotSupported(@NonNull final BluetoothDevice device) {
    }

    private void changeConnState(GattConnection connection, int newState) {
        connection.setConnectionState(newState);

        String stringState;
        switch(newState){
            case DISCONNECTING:
                stringState = "DISCONNECTING";
                break;
            case CONNECTED:
                stringState = "CONNECTED";
                break;
            case NOT_STARTED:
                stringState = "DISCONNECTED";
                break;
            case CONNECTING:
                stringState = "CONNECTING";
                break;
            case DISCOVERED:
                stringState = "DISCOVERED";
                break;
            default:
                stringState = "UNKNOWN #" + newState;
                break;

        }
        Log.i(TAG,"Connection state of device '"+connection.getServerAddr()+"' was changed to: "+stringState);

        broadcastChangedState(connection.getServerAddr());
        updateForegroundNotification();
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, StatusScreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        float averageUpdateRate = 0.0f;
        int connectedDevices = 0;
        for (TagGattManager tag : mConnections.values()) {
            averageUpdateRate += tag.getConnection().getURate();
            int state = tag.getConnection().getConnectionState();
            if (state == CONNECTING || state == CONNECTED || state == DISCOVERED) {
                connectedDevices++;
            }
        }

        if (!mConnections.isEmpty()) {
            averageUpdateRate /= mConnections.size();
        }

        String mqttState = getString(mCachedMqttIsConnected ? R.string.subtitle_mqtt_on : R.string.subtitle_mqtt_off);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_msg, connectedDevices, averageUpdateRate, mqttState))
                .setSmallIcon(R.drawable.ic_device_blinky)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateForegroundNotification() {
        boolean isForegroundNotificationVisible = false;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == ONGOING_NOTIFICATION_ID) {
                isForegroundNotificationVisible = true;
                break;
            }
        }

        if (isForegroundNotificationVisible) {
            notificationManager.notify(ONGOING_NOTIFICATION_ID, buildForegroundNotification());
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, buildForegroundNotification());
        }
    }

    private void broadcastChangedState(String deviceAddress) {
        Intent intent = new Intent(BROADCAST_CHANGED_STATE);
        intent.putExtra("deviceAddress", deviceAddress);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastMqttConnection(boolean isConnected) {
        Intent intent = new Intent(BROADCAST_MQTT_CONNECTION);
        intent.putExtra("connected", isConnected);
        mLocalBroadcastManager.sendBroadcast(intent);
        mCachedMqttIsConnected = isConnected;
    }

    public ObservableMap<String, TagGattManager> getConnections() {
        return mConnections;
    }

    public MqttForwarder getMqtt() {
        return mMqtt;
    }
}
