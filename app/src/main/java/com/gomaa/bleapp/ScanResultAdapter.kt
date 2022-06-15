package com.gomaa.bleapp


import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_scan_result.view.*


class ScanResultAdapter(
    private val items: List<ScanResult>,
    private val mListener: RequestConnectPermissionListener
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        val view: View = inflater.inflate(R.layout.row_scan_result, null)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    inner class ViewHolder(
        private val view: View,
    ) : RecyclerView.ViewHolder(view) {

        fun bind(result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(
                    itemView.context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mListener.requestConnectPermission()
                return
            } else {
                view.device_name.text = result.device.name ?: "Unnamed"
            }
            view.mac_address.text = result.device.address
            view.signal_strength.text = "${result.rssi} dBm"
            view.setOnClickListener { mListener.onDeviceClicked(result) }
        }
    }

}
