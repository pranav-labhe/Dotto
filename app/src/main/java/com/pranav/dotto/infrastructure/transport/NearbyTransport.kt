package com.pranav.dotto.infrastructure.transport

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.pranav.dotto.application.transport.MoveTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class NearbyTransport(private val context: Context) : MoveTransport {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.pranav.dotto.PVP"
    
    private val _connectionState = MutableStateFlow<MoveTransport.ConnectionState>(MoveTransport.ConnectionState.Idle)
    override val connectionState: StateFlow<MoveTransport.ConnectionState> = _connectionState.asStateFlow()

    private var remoteEndpointId: String? = null
    private var levelCallback: ((Int) -> Unit)? = null
    private var moveCallback: ((Int) -> Unit)? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            if (bytes.size < 5) return
            
            val buffer = ByteBuffer.wrap(bytes)
            val type = buffer.get().toInt()
            val value = buffer.getInt()
            
            when (type) {
                1 -> levelCallback?.invoke(value)
                2 -> moveCallback?.invoke(value)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No-op for small payloads
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            _connectionState.value = MoveTransport.ConnectionState.Connecting
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    remoteEndpointId = endpointId
                    _connectionState.value = MoveTransport.ConnectionState.Connected(endpointId, "Opponent")
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                }
                else -> {
                    _connectionState.value = MoveTransport.ConnectionState.Error("Connection failed: ${result.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            remoteEndpointId = null
            _connectionState.value = MoveTransport.ConnectionState.Idle
        }
    }

    override fun sendLevel(levelNumber: Int) {
        sendPayload(1, levelNumber)
    }

    override fun sendMove(cellIndex: Int) {
        sendPayload(2, cellIndex)
    }

    private fun sendPayload(type: Int, value: Int) {
        val endpointId = remoteEndpointId ?: return
        val buffer = ByteBuffer.allocate(5)
        buffer.put(type.toByte())
        buffer.putInt(value)
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(buffer.array()))
    }

    override fun onLevelReceived(callback: (Int) -> Unit) {
        levelCallback = callback
    }

    override fun onMoveReceived(callback: (Int) -> Unit) {
        moveCallback = callback
    }

    override fun startDiscovery() {
        _connectionState.value = MoveTransport.ConnectionState.Discovering
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startDiscovery(serviceId, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                connectionsClient.requestConnection("Player", endpointId, connectionLifecycleCallback)
            }

            override fun onEndpointLost(endpointId: String) {}
        }, discoveryOptions)
            .addOnFailureListener { e ->
                _connectionState.value = MoveTransport.ConnectionState.Error("Discovery failed: ${e.message}")
            }
    }

    override fun startAdvertising(localName: String) {
        _connectionState.value = MoveTransport.ConnectionState.Advertising
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startAdvertising(localName, serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnFailureListener { e ->
                _connectionState.value = MoveTransport.ConnectionState.Error("Advertising failed: ${e.message}")
            }
    }

    override fun disconnect() {
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        remoteEndpointId = null
        _connectionState.value = MoveTransport.ConnectionState.Idle
    }
}
