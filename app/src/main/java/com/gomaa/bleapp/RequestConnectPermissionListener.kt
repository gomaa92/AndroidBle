package com.gomaa.bleapp

import android.bluetooth.le.ScanResult

interface RequestConnectPermissionListener {
    fun requestConnectPermission()
    fun onDeviceClicked(result: ScanResult)
}