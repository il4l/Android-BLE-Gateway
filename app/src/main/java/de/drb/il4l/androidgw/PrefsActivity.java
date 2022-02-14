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

import static de.drb.il4l.androidgw.BLE_GW_Application.MQTTname;
import static de.drb.il4l.androidgw.BLE_GW_Application.MQTTtopic;
import static de.drb.il4l.androidgw.BLE_GW_Application.serverUri;
import static de.drb.il4l.androidgw.BLE_GW_Application.showPOS;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.drb.il4l.androidgw.R;

public class PrefsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.prefs);
        ButterKnife.bind(this);

        TextInputEditText wlsip = findViewById(R.id.WLSip);
        TextInputEditText showPos = findViewById(R.id.showPos);
        TextInputEditText mqttTopic = findViewById(R.id.mqttTopic);
        TextInputEditText mqttName = findViewById(R.id.mqttName);

        wlsip.setText(serverUri);
        showPos.setText(String.valueOf(showPOS));
        mqttTopic.setText(MQTTtopic);
        mqttName.setText(String.valueOf(MQTTname));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.prefs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick(R.id.backButton)
    public void onBackButtonClicked(){
        this.finish();
    }

    @OnClick(R.id.saveButton)
    public void onSaveButtonClicked(){
        TextInputEditText wlsip = findViewById(R.id.WLSip);
        TextInputEditText showPos = findViewById(R.id.showPos);
        TextInputEditText mqttTopic = findViewById(R.id.mqttTopic);
        TextInputEditText mqttName = findViewById(R.id.mqttName);

        Preferences.setPrefs("WLSip", wlsip.getText().toString(),getApplicationContext());
        Preferences.setPrefs("showPos", showPos.getText().toString(),getApplicationContext());
        Preferences.setPrefs("mqttTopic", mqttTopic.getText().toString(),getApplicationContext());
        Preferences.setPrefs("mqttName", mqttName.getText().toString(),getApplicationContext());

        serverUri = wlsip.getText().toString();
        showPOS = Boolean.parseBoolean(showPos.getText()+"");
        MQTTtopic = mqttTopic.getText().toString();
        MQTTname = Boolean.parseBoolean(mqttName.getText()+"");

        Toast toast = Toast.makeText(getApplicationContext(), R.string.settings_saved, Toast.LENGTH_SHORT);
        toast.show();
    }
}
