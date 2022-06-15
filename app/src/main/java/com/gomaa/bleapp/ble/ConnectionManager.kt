package com.gomaa.bleapp.ble

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

const val TAG = "ConnectionManager"

object ConnectionManager {
    private val deviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private var listeners: MutableSet<WeakReference<ConnectionEventListener>> = mutableSetOf()


    fun servicesOnDevice(device: BluetoothDevice): List<BluetoothGattService>? =
        deviceGattMap[device]?.services

    fun registerListener(listener: ConnectionEventListener) {
        if (listeners.map { it.get() }.contains(listener)) {
            return
        }
        listeners.add(WeakReference(listener))
        listeners = listeners.filter { it.get() != null }.toMutableSet()
        Log.d(TAG, "registerListener: Added listener $listener, ${listeners.size} listeners total")
    }

    fun unregisterListener(listener: ConnectionEventListener) {
        // Removing elements while in a loop results in a java.util.ConcurrentModificationException
        var toRemove: WeakReference<ConnectionEventListener>? = null
        listeners.forEach {
            if (it.get() == listener) {
                toRemove = it
            }
        }
        toRemove?.let {
            listeners.remove(it)
            Log.d(
                TAG,
                "unregisterListener: Removed listener ${it.get()}, ${listeners.size} listeners total"
            )
        }
    }

    fun connect(device: BluetoothDevice, context: Context) {
        if (device.isConnected()) {
            Log.d(TAG, "connect: Already connected to ${device.address}!")
        } else {
            doOperation(Connect(device, context.applicationContext))
        }
    }

    fun teardownConnection(device: BluetoothDevice) {
        if (device.isConnected()) {
            doOperation(Disconnect(device))
        } else {
            Log.d(
                TAG,
                "teardownConnection: Not connected to ${device.address}, cannot teardown connection!"
            )
        }
    }

    private fun BluetoothDevice.isConnected() = deviceGattMap.containsKey(this)

    fun readCharacteristic(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected() && characteristic.isReadable()) {
            doOperation(CharacteristicRead(device, characteristic.uuid))
        } else if (!characteristic.isReadable()) {
            Log.d(
                TAG,
                "readCharacteristic: Attempting to read ${characteristic.uuid} that isn't readable!"
            )
        } else if (!device.isConnected()) {
            Log.d(
                TAG,
                "readCharacteristic: Not connected to ${device.address}, cannot perform characteristic read"
            )
        }
    }

    fun writeCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray
    ) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> {
                Log.d(
                    TAG,
                    "writeCharacteristic: Characteristic ${characteristic.uuid} cannot be written to"
                )
                return
            }
        }
        if (device.isConnected()) {
            Log.d(TAG, "writeCharacteristic: $payload")
            doOperation(CharacteristicWrite(device, characteristic.uuid, writeType, payload))
        } else {
            Log.d(
                TAG,
                "writeCharacteristic: Not connected to ${device.address}, cannot perform characteristic write"
            )
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "onConnectionStateChange: connected to $deviceAddress")
                    deviceGattMap[gatt.device] = gatt
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "onConnectionStateChange: disconnected from $deviceAddress")
                    teardownConnection(gatt.device)
                }
            } else {
                Log.d(
                    TAG,
                    "onConnectionStateChange: status $status encountered for $deviceAddress!"
                )

                teardownConnection(gatt.device)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(
                        TAG,
                        "onServicesDiscovered: Discovered ${services.size} services for ${device.address}"
                    )
                    printGattTable()
                    listeners.forEach { it.get()?.onConnectionSetupComplete?.invoke(this) }

                } else {
                    Log.d(
                        TAG,
                        "onServicesDiscovered: Service discovery failed due to status $status"
                    )
                    teardownConnection(gatt.device)
                }
            }
        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d(
                            TAG,
                            "onCharacteristicRead: Read characteristic $uuid | value: ${value.toHexString()}"
                        )
                        listeners.forEach {
                            it.get()?.onCharacteristicRead?.invoke(
                                gatt.device,
                                this
                            )
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.d(TAG, "onCharacteristicRead: Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.d(
                            TAG,
                            "onCharacteristicRead: Characteristic read failed for $uuid, error: $status"
                        )
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d(
                            TAG,
                            "onCharacteristicWrite: Wrote to characteristic $uuid | value: ${value.toHexString()}"
                        )
                        listeners.forEach {
                            it.get()?.onCharacteristicWrite?.invoke(
                                gatt.device,
                                this
                            )
                        }

                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.d(TAG, "onCharacteristicWrite: Write not permitted for $uuid!")

                    }
                    else -> {
                        Log.d(
                            TAG,
                            "onCharacteristicWrite: Characteristic write failed for $uuid, error: $status"
                        )
                    }
                }
            }
        }
    }

    @Synchronized
    private fun doOperation(operation: BleOperationType) {

        // Handle Connect separately from other operations that require device to be connected
        if (operation is Connect) {
            with(operation) {
                Log.d(TAG, "doNextOperation: Connecting to ${device.address}")
                device.connectGatt(context, false, callback)
            }
            return
        }

        // Check BluetoothGatt availability for other operations
        val gatt = deviceGattMap[operation.device]
            ?: this@ConnectionManager.run {
                Log.d(
                    TAG,
                    "doNextOperation: Not connected to ${operation.device.address}! Aborting $operation operation."
                )

                return
            }
        when (operation) {
            is Disconnect -> with(operation) {
                Log.d(TAG, "doNextOperation: Disconnecting from ${device.address}")
                gatt.close()
                deviceGattMap.remove(device)
                listeners.forEach { it.get()?.onDisconnect?.invoke(device) }
            }
            is CharacteristicWrite -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    characteristic.writeType = writeType
                    characteristic.value = payload
                    gatt.writeCharacteristic(characteristic)
                } ?: this@ConnectionManager.run {
                    Log.d(TAG, "doNextOperation: Cannot find $characteristicUuid to write to")
                }
            }
            is CharacteristicRead -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    gatt.readCharacteristic(characteristic)
                } ?: this@ConnectionManager.run {
                    Log.d(TAG, "doNextOperation: Cannot find $characteristicUuid to read from")
                }
            }
        }
    }

}