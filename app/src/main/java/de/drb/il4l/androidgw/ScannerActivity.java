/*
 * Copyright (c) 2018, Nordic Semiconductor
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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.drb.il4l.androidgw.adapter.DevicesAdapter;
import de.drb.il4l.androidgw.adapter.DiscoveredBluetoothDevice;
import de.drb.il4l.androidgw.utils.Utils;
import de.drb.il4l.androidgw.viewmodels.GattConnection;
import de.drb.il4l.androidgw.viewmodels.ScannerStateLiveData;
import de.drb.il4l.androidgw.viewmodels.ScannerViewModel;
import de.drb.il4l.androidgw.R;

import android.content.Context;
import android.widget.Toast;

public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 1022; // random number
    private static final int REQUEST_ACCESS_FINE_LOCATION = 1023; // random number
    private static final int REQUEST_ACCESS_BACKGROUND_LOCATION = 1024; // random number
    private static final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1025;

    private ScannerViewModel mScannerViewModel;
    private DevicesAdapter mDevicesAdapter;

    @BindView(R.id.state_scanning)
    View mScanningView;
    @BindView(R.id.no_devices)
    View mEmptyView;
    @BindView(R.id.no_location_permission)
    View mNoLocationPermissionView;
    @BindView(R.id.action_grant_location_permission)
    Button mGrantPermissionButton;
    @BindView(R.id.action_permission_settings)
    Button mPermissionSettingsButton;
    @BindView(R.id.no_location)
    View mNoLocationView;
    @BindView(R.id.bluetooth_off)
    View mNoBluetoothView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.scan_beacons);

        // Create view model containing utility methods for scanning
        mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);
        mScannerViewModel.getScannerState().observe(this, this::startScan);

        // Configure the recycler view
        final RecyclerView recyclerView = findViewById(R.id.recycler_view_scanned);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mDevicesAdapter = new DevicesAdapter(this, mScannerViewModel.getDevices());
        mDevicesAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mDevicesAdapter);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.filter, menu);
        menu.findItem(R.id.filter_uuid).setChecked(mScannerViewModel.isUuidFilterEnabled());
        menu.findItem(R.id.filter_nearby).setChecked(mScannerViewModel.isNearbyFilterEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.filter_uuid) {
            item.setChecked(!item.isChecked());
            mScannerViewModel.filterByUuid(item.isChecked());
            return true;
        } else if (itemId == R.id.filter_nearby) {
            item.setChecked(!item.isChecked());
            mScannerViewModel.filterByDistance(item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(@NonNull final DiscoveredBluetoothDevice device) {
        addBeacon(device);
        mScannerViewModel.mDevicesLiveData.applyFilter();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ASK_MULTIPLE_PERMISSION_REQUEST_CODE) {
            mScannerViewModel.refresh();
        }
    }

    @OnClick(R.id.action_enable_location)
    public void onEnableLocationClicked() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @OnClick(R.id.action_enable_bluetooth)
    public void onEnableBluetoothClicked() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableIntent);
    }

    @OnClick(R.id.action_grant_location_permission)
    public void onGrantLocationPermissionClicked() {
        Utils.markLocationPermissionRequested(this);
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
    }

    @OnClick(R.id.action_permission_settings)
    public void onPermissionSettingsClicked() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    @OnClick(R.id.backButton)
    public void onBackButtonClicked(){
        this.finish();
    }

    private void addBeacon(DiscoveredBluetoothDevice bt_dev) {
        // Add beacon to the list of known beacons.
        // This will ensure it wont be displayed anymore in the ScannerActivity
        if(bt_dev != null){
            System.out.println("addBeacon " + bt_dev.getName());
            BluetoothDevice ble_dev = bt_dev.getDevice();
            ble_dev.createBond();
            BLE_GW_Application.KnownBeacons.put(ble_dev.getAddress(),ble_dev.getName());
            BLE_GW_Application.saveBeacons(getApplicationContext());

            GatewayService gatewayService = ((BLE_GW_Application) getApplication()).getGatewayService();
            gatewayService.deviceConnect(new GattConnection(bt_dev.getAddress(), 0));

            // send simple notification
            Context context = getApplicationContext();
            CharSequence text = "BLE Beacon "+bt_dev.getAddress()+" added!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private void startScan(final ScannerStateLiveData state) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(this)) {
            mNoLocationPermissionView.setVisibility(View.GONE);

            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                mNoBluetoothView.setVisibility(View.GONE);

                // We are now OK to start scanning
                mScannerViewModel.startScan();
                mScanningView.setVisibility(View.VISIBLE);

                if (!state.hasRecords()) {

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        mNoLocationView.setVisibility(View.INVISIBLE);
                    } else {
                        mNoLocationView.setVisibility(View.VISIBLE);
                    }
                } else {
                    mEmptyView.setVisibility(View.GONE);
                }
            } else {
                mNoBluetoothView.setVisibility(View.VISIBLE);
                mScanningView.setVisibility(View.INVISIBLE);
                mEmptyView.setVisibility(View.GONE);
                clear();
            }
        } else {
            mNoLocationPermissionView.setVisibility(View.VISIBLE);
            mNoBluetoothView.setVisibility(View.GONE);
            mScanningView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            mGrantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            mPermissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private void stopScan() {
        mScannerViewModel.stopScan();
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private void clear() {
        mScannerViewModel.getDevices().clear();
        mScannerViewModel.getScannerState().clearRecords();
    }
}
