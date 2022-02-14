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
package de.drb.il4l.androidgw.mqtt;

import static de.drb.il4l.androidgw.BLE_GW_Application.serverUri;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import no.nordicsemi.android.ble.data.Data;
import de.drb.il4l.androidgw.BLE_GW_Application;
import de.drb.il4l.androidgw.viewmodels.GattConnection;

/**
 * This class is responsible for forwarding BLE messages to a MQTT Broker.
 */
public class MqttForwarder {
    private final static String TAG = MqttForwarder.class.getSimpleName();

    private final MqttAndroidClient mqttAndroidClient;
    private final static int QoS = 2;

    // TODO: add authentication
    private final String username = "xxxxxxx";
    private final String password = "yyyyyyyyyy";

    public MqttForwarder(Context context, String uniqueId, MqttCallbackExtended callback) {
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, "BLE GW " + uniqueId);
        mqttAndroidClient.setCallback(callback);
    }

    public boolean isConnected() {
        return mqttAndroidClient.isConnected();
    }

    public void connect() throws MqttException {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        //mqttConnectOptions.setUserName(username);
        //mqttConnectOptions.setPassword(password.toCharArray());

        mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(true);
                disconnectedBufferOptions.setBufferSize(100);
                disconnectedBufferOptions.setPersistBuffer(false);
                disconnectedBufferOptions.setDeleteOldestMessages(false);
                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                Log.e(TAG, "Failed to connect to: " + serverUri, e);
            }
        });
    }

    public void publish(String topic, MqttMessage message) throws MqttException {
        if (!isConnected()) {
            connect();
        }

        mqttAndroidClient.publish(topic, message);
    }

    public void publishRanging(GattConnection connection, Data data) throws MqttException {
        final String distancesTopic = BLE_GW_Application.MQTTtopic;

        String topic = distancesTopic+"/"+connection.getServerAddr();
        if (BLE_GW_Application.MQTTname){ //user chooses to use the device name as topic
            topic = distancesTopic+"/"+connection.getName();
        }

        MqttMessage message = new MqttMessage(data.getValue());
        message.setQos(QoS);
        publish(topic, message);
    }
}
