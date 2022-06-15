package com.gomaa.bleapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gomaa.bleapp.ble.*
import kotlinx.android.synthetic.main.activity_ble_operations.*


class BleOperationsActivity : AppCompatActivity(), OnCharacteristicClicked {

    private lateinit var device: BluetoothDevice

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics, this) /*{ characteristic ->
            if (characteristic.isWritable() || characteristic.isWritableWithoutResponse())
                ConnectionManager.writeCharacteristic(device, characteristic, "AB".hexToBytes())
        }*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_operations)
        ConnectionManager.registerListener(connectionEventListener)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")
        characteristics_recycler_view.adapter = characteristicAdapter


    }

    override fun onItemClicked(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.isWritable() || characteristic.isWritableWithoutResponse())
            ConnectionManager.writeCharacteristic(device, characteristic, "AB".hexToBytes())
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    Toast.makeText(this@BleOperationsActivity, "Disconnect", Toast.LENGTH_LONG)
                        .show()
                }
            }

            onCharacteristicRead = { _, characteristic ->
                Log.d(
                    TAG,
                    "Read from ${characteristic.uuid}: ${characteristic.value.toHexString()}"
                )
            }

            onCharacteristicWrite = { _, characteristic ->
                Toast.makeText(
                    this@BleOperationsActivity,
                    "Wrote to ${characteristic.uuid}",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "Wrote to ${characteristic.uuid}")
            }
        }
    }

    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

}