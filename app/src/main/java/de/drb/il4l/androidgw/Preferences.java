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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementierung der Preference Lade und Speicherfunktionen für Stringlisten
 *
 * @author  Lars Schymik
 * @version 1.0
 * @since   2019-09-01
 */
public class Preferences {

    public static void setArrayPrefs(String arrayName, ArrayList<String> array, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName +"_size", array.size());
        for(int i=0;i<array.size();i++)
            editor.putString(arrayName + "_" + i, array.get(i));
        editor.apply();
    }

    public static ArrayList<String> getArrayPrefs(String arrayName, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int size = prefs.getInt(arrayName + "_size", 0);
        ArrayList<String> array = new ArrayList<>(size);
        for(int i=0;i<size;i++)
            array.add(prefs.getString(arrayName + "_" + i, null));
        return array;
    }

    public static void setPrefs(String key, String value, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void delPrefs(String key, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }

    public static String getPrefs(String key, Context context){
        return getPrefs(key, context, "notfound");
    }

    public static String getPrefs(String key, Context context, String defaultValue){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

}
