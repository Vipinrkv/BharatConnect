package com.bharatconnect.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * =========================================================================================
 * REAL-TIME NETWORK MONITOR (BHARAT OFFLINE-FIRST RESILIENCE)
 * =========================================================================================
 *
 * Provides a continuous StateFlow<Boolean> indicating whether the device has an active,
 * validated internet connection across 2G/3G/4G/5G/Wi-Fi in India.
 */
object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null

    fun initialize(context: Context) {
        if (connectivityManager != null) return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager = cm

        if (cm != null) {
            _isOnline.value = checkCurrentConnectivity(cm)

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkCurrentConnectivity(cm)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _isOnline.value = hasInternet
                }
            })
        }
    }

    private fun checkCurrentConnectivity(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * For tests or mock environments to simulate network drops
     */
    fun setOnlineForTesting(online: Boolean) {
        _isOnline.value = online
    }
}
