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
package de.drb.il4l.androidgw.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class StatusViewModel extends AndroidViewModel {

	public StatusViewModel(final Application application) {
		super(application);
	}

	private final MutableLiveData<List<GattConnection>> mGattConnections = new MutableLiveData<>();

	private final MutableLiveData<Boolean> mMqttConnection = new MutableLiveData<>();

	public void addConnection(GattConnection connection) {
		List<GattConnection> newList = new ArrayList<>();
		List<GattConnection> oldList = mGattConnections.getValue();
		if (oldList != null) {
			newList.addAll(oldList);
		}
		newList.add(connection);

		setConnections(newList);
	}

	public void setConnections(List<GattConnection> newList) {
		mGattConnections.postValue(newList);
	}

	public MutableLiveData<List<GattConnection>> getConnections() {
		return mGattConnections;
	}

	public MutableLiveData<Boolean> getMqttConnection() {
		return mMqttConnection;
	}
}
