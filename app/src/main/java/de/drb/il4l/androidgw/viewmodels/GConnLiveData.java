package de.drb.il4l.androidgw.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class GConnLiveData extends LiveData<List<GattConnection>> {
    private final static String TAG = GConnLiveData.class.getSimpleName();

    public List<GattConnection> allConnStates;

    public GConnLiveData() {
        this.allConnStates =  new ArrayList<>();
    }

    /**
     * Finds the index of existing devices on the device list.
     *
     * @param serv_addr serv_addr
     * @return Index of -1 if not found.
     *
     */
    private int indexOf(final String serv_addr) {
        int i = 0;
        for (final GattConnection gc : allConnStates) {
            if (gc.getServerAddr().matches(serv_addr))
                return i;
            i++;
        }
        return -1;
    }

    public void addConn(GattConnection newGC){
        allConnStates.add(newGC);
        postValue(allConnStates);
    }

    public void changeState(String server_addr, Integer state){
        for(int i=0;i<allConnStates.size();i++){
            if(allConnStates.get(i).getServerAddr().equals(server_addr)){
                allConnStates.get(i).setConnectionState(state);
            }
        }
        Log.i(TAG,"Connection state was changed, posting to UI ....");
        postValue(allConnStates);
    }

    public void updateURate(String server_addr, float urate){
        for(int i=0;i<allConnStates.size();i++){
            if(allConnStates.get(i).getServerAddr().equals(server_addr)){
                allConnStates.get(i).setURate(urate);
            }
        }
        postValue(allConnStates);
    }
}
