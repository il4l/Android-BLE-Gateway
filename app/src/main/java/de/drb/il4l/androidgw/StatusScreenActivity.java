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

import static de.drb.il4l.androidgw.BLE_GW_Application.KnownBeacons;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.ObservableMap;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.List;

import de.drb.il4l.androidgw.adapter.BeaconAdapter;
import de.drb.il4l.androidgw.viewmodels.GattConnection;
import de.drb.il4l.androidgw.viewmodels.StatusViewModel;
public class StatusScreenActivity extends AppCompatActivity implements BeaconAdapter.OnItemClickListener {

    private final static String TAG = StatusScreenActivity.class.getSimpleName();

    private StatusViewModel mStatusViewModel;
    public static BeaconAdapter beaconAdapter;
    private BroadcastReceiver broadcastReceiver;

    // Observe service connection changes
    private ObservableMap.OnMapChangedCallback<ObservableMap<String, TagGattManager>, String, TagGattManager> mObserveCallback = new ObservableMap.OnMapChangedCallback<ObservableMap<String, TagGattManager>, String, TagGattManager>() {
        @Override
        public void onMapChanged(ObservableMap<String, TagGattManager> sender, String key) {
            List<GattConnection> connections = new ArrayList<>(sender.size() + KnownBeacons.size());
            for (TagGattManager tag: sender.values()) {
                connections.add(tag.getConnection());
            }
            for (String deviceAddress: KnownBeacons.keySet()) {
                if (!sender.containsKey(deviceAddress)) {
                    connections.add(new GattConnection(deviceAddress, GattConnection.NOT_STARTED));
                }
            }
            mStatusViewModel.setConnections(connections);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatusViewModel = ViewModelProviders.of(this).get(StatusViewModel.class);

        setContentView(R.layout.activity_status_screen);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.status_title);
        toolbar.setSubtitle(
                getString(mStatusViewModel.getMqttConnection().getValue() == Boolean.TRUE ? R.string.subtitle_mqtt_on : R.string.subtitle_mqtt_off)
        );
        setSupportActionBar(toolbar);

        // Configure the recycler view
        final RecyclerView recyclerView = findViewById(R.id.recycler_view_status);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        beaconAdapter = new BeaconAdapter(this);
        beaconAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(beaconAdapter);
        mStatusViewModel.getConnections().observe(this, beaconAdapter::submitList);
        //badapter.notifyItemChanged(1, null);

        // Add handler for beacon removal via swipe
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        // Remove item from backing list here
                        int position = viewHolder.getAdapterPosition();
                        beaconAdapter.deleteItem(position);
                    }
                }
        );
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Link with service state changes
        getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                GatewayService gatewayService = ((BLE_GW_Application) getApplication()).getGatewayService();
                if (event != Lifecycle.Event.ON_DESTROY) {
                    gatewayService.getConnections().addOnMapChangedCallback(mObserveCallback);
                } else {
                    gatewayService.getConnections().removeOnMapChangedCallback(mObserveCallback);
                }

                // manually initialize on start
                if (event == Lifecycle.Event.ON_START) {
                    mObserveCallback.onMapChanged(gatewayService.getConnections(), null);
                }
            }
        });

        // Listen for BLE device state changes (issued from GatewayService) in order to
        // notify our BeaconAdapter.
        broadcastReceiver = new BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GatewayService.BROADCAST_CHANGED_STATE.equals(intent.getAction())) {
                    // Try to find device
                    String deviceAddress = intent.getStringExtra("deviceAddress");
                    int position = beaconAdapter.getPositionOfDevice(deviceAddress);
                    if (position >= 0) {
                        beaconAdapter.notifyItemChanged(position);
                    } else {
                        // We can't find the device/beacon that changed so just refresh everything..
                        beaconAdapter.notifyDataSetChanged();
                    }
                } else if (GatewayService.BROADCAST_MQTT_CONNECTION.equals(intent.getAction())) {
                    boolean mqttActive = intent.getBooleanExtra("connected", false);
                    if (mqttActive) {
                        getSupportActionBar().setSubtitle(getString(R.string.subtitle_mqtt_on));
                    } else {
                        getSupportActionBar().setSubtitle(getString(R.string.subtitle_mqtt_off));
                    }
                    mStatusViewModel.getMqttConnection().postValue(mqttActive);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GatewayService.BROADCAST_CHANGED_STATE);
        filter.addAction(GatewayService.BROADCAST_MQTT_CONNECTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onRestart() {
        super.onRestart();
        // Rebuild view if scanner activity is re-opened
        beaconAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.scanButton) {
            Intent scannerIntent = new Intent(this, ScannerActivity.class);
            startActivity(scannerIntent);
        } else if (itemId == R.id.prefsButton) {
            Intent prefsIntent = new Intent(this, PrefsActivity.class);
            startActivity(prefsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(@NonNull GattConnection gattConnection) {
        GatewayService gatewayService = ((BLE_GW_Application) getApplication()).getGatewayService();

        if(gattConnection.getConnectionState()>0){
            Log.i(TAG, "User wants to disconnect with device " + gattConnection.getServerAddr());
            gatewayService.deviceDisconnect(gattConnection);
            // write action to prefs - delete autoconnect to this device
            Preferences.delPrefs("reconnectTag", getApplicationContext());
        } else {
            // connect to device
            Log.i(TAG, "User wants to connect to device " + gattConnection.getServerAddr());
            gatewayService.deviceConnect(gattConnection);
            // write action to prefs - after restart app will automatically connect to this device
            Preferences.setPrefs("reconnectTag", gattConnection.getServerAddr(), getApplicationContext());
        }
    }
}
