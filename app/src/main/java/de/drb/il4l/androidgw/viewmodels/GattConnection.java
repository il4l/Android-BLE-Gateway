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
package de.drb.il4l.androidgw.viewmodels;

import android.os.Parcel;
import android.os.Parcelable;

import de.drb.il4l.androidgw.BLE_GW_Application;

public class GattConnection implements Parcelable {

    /* Service ConnStates:

        mögliche Zusände:
           -1 - beende Verbindung
            0 - nicht gestartet
            1 - verbinde zu GATT Server
            2 - verbunden zu GATT Server
            3 - Services entdeckt / fertig
         */
    public static final int DISCONNECTING = -1;
    public static final int NOT_STARTED = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;
    public static final int DISCOVERED = 3;

    private String serverAddr;
    private Integer connectionState;
    private float urate;

    public GattConnection(String serverAddress, Integer connectionState) {
        this.serverAddr = serverAddress;
        this.connectionState = connectionState;
        this.urate = 0.0f;
    }

    protected GattConnection(Parcel in) {
        serverAddr = in.readString();
        if (in.readByte() == 0) {
            connectionState = null;
        } else {
            connectionState = in.readInt();
        }
    }

    public static final Creator<GattConnection> CREATOR = new Creator<GattConnection>() {
        @Override
        public GattConnection createFromParcel(Parcel in) {
            return new GattConnection(in);
        }

        @Override
        public GattConnection[] newArray(int size) {
            return new GattConnection[size];
        }
    };

    public Integer getConnectionState(){
        return connectionState;
    }

    public void setConnectionState(Integer newState){
        connectionState = newState;
    }

    public void setURate(float urate){
        this.urate = urate;
    }

    public float getURate(){
        return  urate;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String newAddr){
        serverAddr = newAddr;
    }

    public String getName() {
        // Try to get bluetooth name and fallback to device address if we can't figure out the name.
        String bleDevName = BLE_GW_Application.KnownBeacons.get(serverAddr);
        if (bleDevName != null) {
            String[] nrList = bleDevName.split("_");
            String nr = null;
            for (String s : nrList) {
                if (s.length()==4) {
                    nr = s;
                }
            }
            if (nr != null) bleDevName = nr;
        } else {
            bleDevName = serverAddr;
        }
        return bleDevName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        parcel.writeString(serverAddr);
        parcel.writeInt(connectionState);
    }
}
