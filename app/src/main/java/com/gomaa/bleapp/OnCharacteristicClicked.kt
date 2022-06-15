package com.gomaa.bleapp

import android.bluetooth.BluetoothGattCharacteristic

interface OnCharacteristicClicked {
    fun onItemClicked(characteristic: BluetoothGattCharacteristic)
}