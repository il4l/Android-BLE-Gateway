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

package de.drb.il4l.androidgw.profile.callback;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

@SuppressWarnings("ConstantConditions")
public abstract class RangeDataCallback implements ProfileDataCallback, RangeCallback {
    private static final int STATE_RELEASED = 0x00;
    private static final int STATE_PRESSED = 0x01;

    public static final String TAG = RangeDataCallback.class.getSimpleName();

    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        if (data.size() <= 2) {
            onInvalidDataReceived(device, data);
            return;
        }

        Log.v(TAG,"data size received: "+data.size());

        if (data != null){
            int valueCount = data.getIntValue(Data.FORMAT_UINT16, 0);
            Log.d(TAG,"RangeDataCallback with "+valueCount+" values");
            /*for(int i=0;i<valueCount;i++){
                int liteID = data.getIntValue(Data.FORMAT_UINT16,i*4+2);
                int range = data.getIntValue(Data.FORMAT_UINT16, i*4+4);
                if (range != 0xFFFF) {
                    onRangeChanged(device, range, liteID);
                } else {
                    onInvalidDataReceived(device, data);
                }
            }*/
            onRangeChanged(device,data);
        }

    }


}
