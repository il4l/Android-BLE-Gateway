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
package de.drb.il4l.androidgw.adapter;

import static de.drb.il4l.androidgw.BLE_GW_Application.KnownBeacons;
import static de.drb.il4l.androidgw.BLE_GW_Application.saveBeacons;
import static de.drb.il4l.androidgw.BLE_GW_Application.showPOS;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.drb.il4l.androidgw.BLE_GW_Application;
import de.drb.il4l.androidgw.GatewayService;
import de.drb.il4l.androidgw.R;
import de.drb.il4l.androidgw.StatusScreenActivity;
import de.drb.il4l.androidgw.viewmodels.GattConnection;

/**
 * Provides beacon data bindings for recyclerview data.
 */
public class BeaconAdapter extends RecyclerView.Adapter<BeaconAdapter.ViewHolder> {
    private List<GattConnection> mConnections;
    private OnItemClickListener mOnItemClickListener;
    private final StatusScreenActivity mActivity;

    private GattConnection recentlyDeletedItem;
    private int recentlyDeletedItemPos;
    private String recentlyDeletedItemAddr;
    private String recentlyDeletedItemName;

    @ColorInt private static final int lightGreen = 0xFF00FF00;
    @ColorInt private static final int darkGreen = 0xff009933;
    @ColorInt private static final int orange = 0xffff9900;

    // Provide a suitable constructor (depends on the kind of dataset)
    public BeaconAdapter(final StatusScreenActivity activity) {
        mActivity = activity;
        setHasStableIds(true);
    }

    public void submitList(List<GattConnection> newList) {
        List<GattConnection> oldList = mConnections;
        mConnections = newList;
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new GattConnDiffCallback(oldList, newList));
                /*new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                if (oldList != null) {
                    return oldList.size();
                }
                return 0;
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                GattConnection a = oldList != null ? oldList.get(oldItemPosition) : null;
                GattConnection b = newList.get(oldItemPosition);
                return b.equals(a);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return areItemsTheSame(oldItemPosition, newItemPosition);
            }
        });*/
        result.dispatchUpdatesTo(this);
    }

    public int getPositionOfDevice(String deviceAddress) {
        for (int pos = 0; pos < getItemCount(); ++pos) {
            if (deviceAddress.equals(mConnections.get(pos).getServerAddr())) {
                return pos;
            }
        }
        return -1;
    }

    @Override
    public long getItemId(final int position) {
        return mConnections.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        if (mConnections != null) {
            return mConnections.size();
        }
        return 0;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(mActivity)
                .inflate(R.layout.beacon_item, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from dataset at this position
        // - replace the contents of the view with that element
        final GattConnection gconn = mConnections.get(position);
        //get the nr
        String nr = gconn.getServerAddr();
        String name = gconn.getName();

        if (nr == name) {
            holder.deviceName.setText(mActivity.getString(R.string.status_tag_addr, nr));
        } else {
            holder.deviceName.setText(mActivity.getString(R.string.status_tag_name, gconn.getName()));
        }

        if (showPOS){
            holder.deviceAddr.setText(BLE_GW_Application.debugPos);
        } else {
            holder.deviceAddr.setText(gconn.getServerAddr());
        }

        holder.connURate.setText(mActivity.getString(R.string.status_urate, gconn.getURate()));
        String connState = "";
        switch(gconn.getConnectionState()){
            case -1:
                connState = mActivity.getString(R.string.state_disconnecting);
                gconn.setURate(0.0f);
                break;
            case 0:
                connState = mActivity.getString(R.string.state_not_started);
                gconn.setURate(0.0f);
                break;
            case 1:
                connState = mActivity.getString(R.string.state_connecting);
                break;
            case 2:
                connState = mActivity.getString(R.string.state_connected);
                break;
            case 3:
                connState = mActivity.getString(R.string.state_services_found);
                break;
            default:
                connState = "NO CASE";
                break;
        }
        holder.connStateText.setText(connState);
        Drawable circle = holder.connStateCircle.getBackground();
        int colorToSet =0;


        switch (gconn.getConnectionState()){
            case -1:
                colorToSet = Color.RED;
                break;
            case 0:
                colorToSet = Color.TRANSPARENT;
                break;
            case 1:
                colorToSet = orange;
                break;
            case 2:
                colorToSet = lightGreen;
                break;
            case 3:
                colorToSet = darkGreen;
                break;
        }

        if (circle instanceof ShapeDrawable) {
            // cast to 'ShapeDrawable'
            ShapeDrawable shapeDrawable = (ShapeDrawable) circle;
            shapeDrawable.getPaint().setColor(colorToSet);
        } else if (circle instanceof GradientDrawable) {
            // cast to 'GradientDrawable'
            GradientDrawable gradientDrawable = (GradientDrawable) circle;
            gradientDrawable.setColor(colorToSet);
        } else if (circle instanceof ColorDrawable) {
            // alpha value may need to be set again after this call
            ColorDrawable colorDrawable = (ColorDrawable) circle;
            colorDrawable.setColor(colorToSet);
        }
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.device_name) TextView deviceName;
        @BindView(R.id.device_address) TextView deviceAddr;
        @BindView(R.id.conn_state_text) TextView connStateText;
        @BindView(R.id.connection_state) ImageView connStateCircle;
        @BindView(R.id.dev_urate) TextView connURate;
        // each data item is just a string in this case

        private ViewHolder(@NonNull final View view) {
            super(view);
            ButterKnife.bind(this, view);

            view.findViewById(R.id.beacon_item).setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    try {
                        mOnItemClickListener.onItemClick(mConnections.get(getAdapterPosition()));
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }
                }
            });
        }
    }

    public void deleteItem(int pos){
        recentlyDeletedItem = mConnections.get(pos);
        recentlyDeletedItemPos = pos;
        recentlyDeletedItemAddr = mConnections.get(pos).getServerAddr();
        recentlyDeletedItemName = KnownBeacons.get(recentlyDeletedItemAddr);
        mConnections.remove(pos);
        KnownBeacons.remove(recentlyDeletedItemAddr);
        showUndoSnackbar();
        saveBeacons(mActivity);
        notifyItemRemoved(pos);
    }

    private void showUndoSnackbar() {
        View view = mActivity.findViewById(R.id.constraint_main_layout);
        Snackbar snackbar = Snackbar.make(view, R.string.msg_tag_deleted, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.msg_tag_delete_undo, v -> undoDelete());
        snackbar.setDuration(5000);
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    String msg = mActivity.getString(R.string.msg_tag_undo_done);
                    Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
                } else if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    String msg = mActivity.getString(R.string.msg_tag_delete_done);
                    Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
                    GatewayService gatewayService = ((BLE_GW_Application) mActivity.getApplication()).getGatewayService();
                    gatewayService.deviceDisconnect(recentlyDeletedItem);
                }
            }

            @Override
            public void onShown(Snackbar snackbar) {
            }
        });
        snackbar.show();
    }

    private void undoDelete() {
        mConnections.add(recentlyDeletedItemPos,
                recentlyDeletedItem);
        KnownBeacons.put(recentlyDeletedItemAddr,recentlyDeletedItemName);
        saveBeacons(mActivity);

        notifyItemInserted(recentlyDeletedItemPos);
    }

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(@NonNull final GattConnection gattConn);

    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
}
