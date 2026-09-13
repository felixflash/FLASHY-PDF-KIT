package com.flashypdfkit.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkMonitor {
    private const val TAG = "NetworkMonitor"
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var isRegistered = false

    fun start(context: Context) {
        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            _isOnline.value = true
            return
        }

        // Set initial state
        _isOnline.value = checkCurrentConnectivity(connectivityManager)

        if (isRegistered) return

        try {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = connectivityManager.getNetworkCapabilities(network)
                    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    Log.d(TAG, "Network available. Has internet: $hasInternet")
                    _isOnline.value = hasInternet
                }

                override fun onLost(network: Network) {
                    val currentOnline = checkCurrentConnectivity(connectivityManager)
                    Log.d(TAG, "Network lost. Current online: $currentOnline")
                    _isOnline.value = currentOnline
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                             caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
                    _isOnline.value = hasInternet
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, callback)
            }
            isRegistered = true
            Log.d(TAG, "Network callback registered successfully. Initial online state: ${_isOnline.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            _isOnline.value = checkCurrentConnectivity(connectivityManager)
        }
    }

    private fun checkCurrentConnectivity(connectivityManager: ConnectivityManager): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // default to true on exception to avoid blocking unnecessarily
        }
    }
}
