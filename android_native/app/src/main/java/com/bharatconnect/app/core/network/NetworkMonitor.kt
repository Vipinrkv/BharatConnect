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

    private val _isMetered = MutableStateFlow(true)
    val isMetered: StateFlow<Boolean> = _isMetered.asStateFlow()

    private val _connectionType = MutableStateFlow("CELLULAR")
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null

    fun initialize(context: Context) {
        if (connectivityManager != null) return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager = cm

        if (cm != null) {
            updateNetworkState(cm)

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    updateNetworkState(cm)
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkCurrentConnectivity(cm)
                    if (!_isOnline.value) {
                        _connectionType.value = "NONE"
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _isOnline.value = hasInternet
                    _isMetered.value = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    _connectionType.value = when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                        else -> "OTHER"
                    }
                }
            })
        }
    }

    private fun updateNetworkState(cm: ConnectivityManager) {
        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            _isOnline.value = false
            _connectionType.value = "NONE"
            return
        }
        val caps = cm.getNetworkCapabilities(activeNetwork)
        if (caps == null) {
            _isOnline.value = false
            _connectionType.value = "NONE"
            return
        }
        _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isMetered.value = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        _connectionType.value = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> "OTHER"
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
    fun setOnlineForTesting(online: Boolean, metered: Boolean = false, type: String = "WIFI") {
        _isOnline.value = online
        _isMetered.value = metered
        _connectionType.value = type
    }
}
