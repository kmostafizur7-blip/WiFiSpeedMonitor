package com.example.wifispeedmonitor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<DeviceInfo> devices;

    public DeviceAdapter(List<DeviceInfo> devices) {
        this.devices = devices;
    }

    public void updateDevices(List<DeviceInfo> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceInfo device = devices.get(position);
        holder.tvName.setText("📱 " + device.name);
        holder.tvIp.setText("IP: " + device.ip);
        holder.tvMac.setText("MAC: " + device.mac);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIp, tvMac;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvIp   = itemView.findViewById(R.id.tvDeviceIp);
            tvMac  = itemView.findViewById(R.id.tvDeviceMac);
        }
    }
}
