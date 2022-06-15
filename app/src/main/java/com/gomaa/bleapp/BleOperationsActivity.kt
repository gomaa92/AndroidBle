package com.gomaa.bleapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gomaa.bleapp.ble.ConnectionManager
import com.gomaa.bleapp.ble.hexToBytes
import com.gomaa.bleapp.ble.isWritable
import com.gomaa.bleapp.ble.isWritableWithoutResponse
import kotlinx.android.synthetic.main.activity_ble_operations.*

const val Characteristic_UUID = "0000fee6-0000-1000-8000-00805f9b34fb"

class BleOperationsActivity : AppCompatActivity(),OnCharacteristicClicked {

    private lateinit var device: BluetoothDevice

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics,this) /*{ characteristic ->
            if (characteristic.isWritable() || characteristic.isWritableWithoutResponse())
                ConnectionManager.writeCharacteristic(device, characteristic, "AB".hexToBytes())
        }*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_operations)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")
        characteristics_recycler_view.adapter = characteristicAdapter


    }

    override fun onItemClicked(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.isWritable() || characteristic.isWritableWithoutResponse())
            ConnectionManager.writeCharacteristic(device, characteristic, "AB".hexToBytes())
    }

}