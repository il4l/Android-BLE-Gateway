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

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Der Inloc4Log BLE Gateway leitet BLE GATT Ranging Service Notifications
 * automatisch per MQTT an den Warehouse Location Server weiter.
 * Die IP des WLS ist in den Einstellungen der App konfigurierbar.
 *
 * @author  Lars Schymik
 * @author  Michael Pekar
 * @version 1.1
 */

public class BLE_GW_Application extends Application {

    static public HashMap<String,String> KnownBeacons;

    public static final String CHANNEL_ID = "GATTServiceChannel";
    public static final int SEND_NOTIFY_DELAY = 3000;

    public static String serverUri = "tcp://192.168.222.237:1883";
    public static boolean showPOS = false;
    public static String MQTTtopic = "POS";
    public static boolean MQTTname = true;

    public static String debugPos = "";

    private GatewayService mGatewayService;
    private ServiceConnection mGatewayServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            GatewayService.LocalBinder localBinder = (GatewayService.LocalBinder) binder;
            mGatewayService = localBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // TODO: disconnect devices?
        }
    };

    public GatewayService getGatewayService() {
        return mGatewayService;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Added to support vector drawables for devices below android 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }

        if(KnownBeacons == null){
            KnownBeacons = new HashMap<>();
        }

        // Load known beacon addresses from prefs..
        Context context = getApplicationContext();
        ArrayList<String> beaconsList = Preferences.getArrayPrefs("MyDevs", context);
        HashMap<String,String> beaconsMap = new HashMap<>();
        try {
            for (String beacon: beaconsList) {
                beaconsMap.put(beacon.split(",")[0], beacon.split(",")[1]);
            }
        } catch (ArrayIndexOutOfBoundsException e){
            Preferences.delPrefs("MyDevs", getApplicationContext());
        }
        KnownBeacons = beaconsMap;

        showPOS = Boolean.parseBoolean(Preferences.getPrefs("showPos", context, String.valueOf(showPOS)));

        String wlsip = Preferences.getPrefs("WLSip", context);
        if(!wlsip.equals("notfound")) {
            serverUri = wlsip;
        }

        for(String cur_addr : KnownBeacons.keySet()) {
            System.out.println("Beacon mit BLE Adresse: "+cur_addr+" aus Prefs geladen");
        }

        MQTTtopic = Preferences.getPrefs("mqttTopic", context, MQTTtopic);
        MQTTname = Boolean.parseBoolean(Preferences.getPrefs("mqttName", context, String.valueOf(MQTTname)));

        createNotificationChannel();

        Intent intent = new Intent(this, GatewayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        bindService(intent, mGatewayServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "GATT Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    static public void saveBeacons(Context context) {
        ArrayList<String> beacons = new ArrayList<String>();
        for (Map.Entry<String,String> b : KnownBeacons.entrySet()){
            beacons.add(b.getKey() + "," + b.getValue());
        }
        Preferences.setArrayPrefs("MyDevs",beacons, context);
    }
}
