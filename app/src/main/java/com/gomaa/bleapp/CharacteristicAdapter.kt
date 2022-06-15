package com.gomaa.bleapp

import android.bluetooth.BluetoothGattCharacteristic
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gomaa.bleapp.ble.printProperties
import kotlinx.android.synthetic.main.row_characteristic.view.*

class CharacteristicAdapter(
    private var items: List<BluetoothGattCharacteristic>,
    private val mListener: OnCharacteristicClicked
) : RecyclerView.Adapter<CharacteristicAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view: View = inflater.inflate(R.layout.row_characteristic, null)

        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    inner class ViewHolder(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {

        fun bind(characteristic: BluetoothGattCharacteristic) {
            view.characteristic_uuid.text = characteristic.uuid.toString()
            view.characteristic_properties.text = characteristic.printProperties()
            view.setOnClickListener { mListener.onItemClicked(characteristic) }
        }
    }
}