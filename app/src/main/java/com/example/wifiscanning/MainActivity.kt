package com.example.wifiscanning

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication3.R
import java.util.*

class MainActivity : AppCompatActivity() {
    private var wifiManager: WifiManager? = null
    private var buttonState: Button? = null
    private var buttonScan: Button? = null
    private var editTextPassword: EditText? = null
    private var linearLayoutScanResults: LinearLayout? = null
    private var textViewScanResults: TextView? = null
    private var wifiReceiver: WifiBroadcastReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // Instantiate broadcast receiver
        wifiReceiver = WifiBroadcastReceiver()

        // Register the receiver
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        //
        buttonState = findViewById<View>(R.id.button_state) as Button
        buttonScan = findViewById<View>(R.id.button_scan) as Button
        editTextPassword = findViewById<View>(R.id.editText_password) as EditText
        textViewScanResults = findViewById<View>(R.id.textView_scanResults) as TextView
        linearLayoutScanResults = findViewById<View>(R.id.linearLayout_scanResults) as LinearLayout
        buttonState!!.setOnClickListener { showWifiState() }
        buttonScan!!.setOnClickListener { askAndStartScanWifi() }
    }

    private fun askAndStartScanWifi() {

        // With Android Level >= 23, you have to ask the user
        // for permission to Call.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 23
            val permission1 =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

            // Check for permissions
            if (permission1 != PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Requesting Permissions")

                // Request permissions
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE
                    ), MY_REQUEST_CODE
                )
                return
            }
            Log.d(LOG_TAG, "Permissions Already Granted")
        }
        doStartScanWifi()
    }

    private fun doStartScanWifi() {
        wifiManager!!.startScan()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(LOG_TAG, "onRequestPermissionsResult")
        when (requestCode) {
            MY_REQUEST_CODE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Log.d(LOG_TAG, "Permission Granted: " + permissions[0])

                    // Start Scan Wifi.
                    doStartScanWifi()
                } else {
                    // Permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(LOG_TAG, "Permission Denied: " + permissions[0])
                }
            }
        }
    }

    private fun showWifiState() {
        val state = wifiManager!!.wifiState
        var statusInfo = "Unknown"
        statusInfo = when (state) {
            WifiManager.WIFI_STATE_DISABLING -> "Disabling"
            WifiManager.WIFI_STATE_DISABLED -> "Disabled"
            WifiManager.WIFI_STATE_ENABLING -> "Enabling"
            WifiManager.WIFI_STATE_ENABLED -> "Enabled"
            WifiManager.WIFI_STATE_UNKNOWN -> "Unknown"
            else -> "Unknown"
        }
        Toast.makeText(this, "Wifi Status: $statusInfo", Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        unregisterReceiver(wifiReceiver)
        super.onStop()
    }

    // Define class to listen to broadcasts
    internal inner class WifiBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(LOG_TAG, "onReceive()")
            Toast.makeText(this@MainActivity, "Scan Complete!", Toast.LENGTH_SHORT).show()
            val ok = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (ok) {
                Log.d(LOG_TAG, "Scan OK")
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                val list = wifiManager!!.scanResults
                showNetworks(list)
                showNetworksDetails(list)
            } else {
                Log.d(LOG_TAG, "Scan not OK")
            }
        }
    }

    private fun showNetworks(results: List<ScanResult>) {
        linearLayoutScanResults!!.removeAllViews()
        for (result in results) {
            val networkCapabilities = result.capabilities
            val networkSSID = result.SSID // Network Name.
            //
            val button = Button(this)
            button.text = "$networkSSID ($networkCapabilities)"
            linearLayoutScanResults!!.addView(button)
            button.setOnClickListener {
                val networkCapabilities = result.capabilities
                connectToNetwork(networkCapabilities, networkSSID)
            }
        }
    }

    private fun showNetworksDetails(results: List<ScanResult>) {
        textViewScanResults!!.text = ""
        val sb = StringBuilder()
        sb.append("Result Count: " + results.size)
        for (i in results.indices) {
            val result = results[i]
            sb.append(
                """

  --------- Network $i/${results.size} ---------"""
            )
            sb.append(
                """
 result.capabilities: ${result.capabilities}"""
            )
            sb.append(
                """
 result.SSID: ${result.SSID}"""
            ) // Network Name.
            sb.append(
                """
 result.BSSID: ${result.BSSID}"""
            )
            sb.append(
                """
 result.frequency: ${result.frequency}"""
            )
            sb.append(
                """
 result.level: ${result.level}"""
            )
            sb.append(
                """
 result.describeContents(): ${result.describeContents()}"""
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) { // Level 17, Android 4.2
                sb.append(
                    """
 result.timestamp: ${result.timestamp}"""
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Level 23, Android 6.0
                sb.append(
                    """
 result.centerFreq0: ${result.centerFreq0}"""
                )
                sb.append(
                    """
 result.centerFreq1: ${result.centerFreq1}"""
                )
                sb.append(
                    """
 result.venueName: ${result.venueName}"""
                )
                sb.append(
                    """
 result.operatorFriendlyName: ${result.operatorFriendlyName}"""
                )
                sb.append(
                    """
 result.channelWidth: ${result.channelWidth}"""
                )
                sb.append(
                    """
 result.is80211mcResponder(): ${result.is80211mcResponder}"""
                )
                sb.append(
                    """
 result.isPassportNetwork(): ${result.isPasspointNetwork}"""
                )
            }
        }
        textViewScanResults!!.text = sb.toString()
    }

    private fun connectToNetwork(networkCapabilities: String, networkSSID: String) {
        Toast.makeText(this, "Connecting to network: $networkSSID", Toast.LENGTH_SHORT).show()
        val networkPass = editTextPassword!!.text.toString()
        //
        val wifiConfig = WifiConfiguration()
        wifiConfig.SSID = "\"" + networkSSID + "\""
        if (networkCapabilities.uppercase(Locale.getDefault()).contains("WEP")) { // WEP Network.
            Toast.makeText(this, "WEP Network", Toast.LENGTH_SHORT).show()
            wifiConfig.wepKeys[0] = "\"" + networkPass + "\""
            wifiConfig.wepTxKeyIndex = 0
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
        } else if (networkCapabilities.uppercase(Locale.getDefault())
                .contains("WPA")
        ) { // WPA Network
            Toast.makeText(this, "WPA Network", Toast.LENGTH_SHORT).show()
            wifiConfig.preSharedKey = "\"" + networkPass + "\""
        } else { // OPEN Network.
            Toast.makeText(this, "OPEN Network", Toast.LENGTH_SHORT).show()
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        }
        wifiManager!!.addNetwork(wifiConfig)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        val list = wifiManager!!.configuredNetworks
        for (config in list) {
            if (config.SSID != null && config.SSID == "\"" + networkSSID + "\"") {
                wifiManager!!.disconnect()
                wifiManager!!.enableNetwork(config.networkId, true)
                wifiManager!!.reconnect()
                break
            }
        }
    }

    companion object {
        private const val LOG_TAG = "AndroidExample"
        private const val MY_REQUEST_CODE = 123
    }
}