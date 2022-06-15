package com.gomaa.bleapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

/*fun requestPermission(context: Context, permission: String) {
    when(Build.VERSION.SDK_INT){
        Build.VERSION_CODES.S->{
            //todo request android 12 ble permissions
        }
        else->{
            //todo request android < 12 ble permissions
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    ) {

        requestBluetoothScan.launch(Manifest.permission.BLUETOOTH_SCAN)
        return
    }
}
interface PermissionRequest{
    fun requestScanPermission()
    fun requestConnectPermission()
    fun requestLocationPermission()
}*/

