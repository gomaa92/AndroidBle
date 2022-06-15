package com.gomaa.bleapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.gomaa.bleapp.ble.ConnectionEventListener
import com.gomaa.bleapp.ble.ConnectionManager
import com.gomaa.bleapp.ble.hasPermission
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), RequestConnectPermissionListener {
    private val enableGpsIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    private val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { scan_button.text = if (value) "Stop Scan" else "Start Scan" }
        }
    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults, this)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) {
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scanResultRecyclerView.adapter = scanResultAdapter
        scan_button.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }

    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {

            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestBluetoothScan.launch(Manifest.permission.BLUETOOTH_SCAN)
                    return
                }
            }

            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true


        }
    }

    private fun stopBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothScan.launch(Manifest.permission.BLUETOOTH_SCAN)
                return
            }
        }
        bleScanner.stopScan(scanCallback)
        isScanning = false

    }

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                Intent(this@MainActivity, BleOperationsActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    startActivity(it)
                }
                ConnectionManager.unregisterListener(this)
            }
            onDisconnect = {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Disconnect", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestBluetoothPermissionConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    return
                }
            }

            requestBluetoothEnable.launch(enableBtIntent)
        }
    }

    private var requestBluetoothScan =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startBleScan()
            }
        }
    private var requestBluetoothPermissionConnect =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                promptEnableBluetooth()
            }
        }
    private var requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startBleScan()
                return@registerForActivityResult
            }
            requestLocationPermission()
        }
    private var requestBluetoothEnable =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                promptEnableBluetooth()
            }
        }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            AlertDialog.Builder(this).setTitle("Location permission required").setMessage(
                "the system requires apps to be granted location access in order to scan for BLE devices."
            ).setCancelable(false).setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }.show()

        }
    }

    override fun requestConnectPermission() {
        requestBluetoothPermissionConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    override fun onDeviceClicked(result: ScanResult) {
        if (isScanning) {
            stopBleScan()
        }
        ConnectionManager.connect(result.device, this)
    }


    private fun isLocationEnable(): Boolean {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /*private fun enableGPS() {
        runOnUiThread {
            AlertDialog.Builder(this).setTitle("Location Enable required").setMessage(
                "Enable location is required to proceed"
            ).setCancelable(false).setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                enableGpsRequest.launch(enableGpsIntent)
            }.show()

        }
    }*/

    /*private val enableGpsRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) {
                startBleScan()
                return@registerForActivityResult
            }
            enableGPS()


        }*/
}