package com.pranav.dotto.infrastructure.transport

import android.content.Context
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
    private var startCallback: (() -> Unit)? = null
    private var restartCallback: (() -> Unit)? = null
    private var quitCallback: (() -> Unit)? = null
    private var moveCallback: ((Int, Int, Int) -> Unit)? = null
    
    private val discoveredEndpoints = mutableListOf<MoveTransport.DiscoveredEndpoint>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            if (bytes.isEmpty()) return
            
            val buffer = ByteBuffer.wrap(bytes)
            val type = buffer.get().toInt()
            
            when (type) {
                1 -> if (bytes.size >= 5) levelCallback?.invoke(buffer.getInt())
                2 -> if (bytes.size >= 13) moveCallback?.invoke(buffer.getInt(), buffer.getInt(), buffer.getInt())
                3 -> startCallback?.invoke()
                4 -> restartCallback?.invoke()
                5 -> quitCallback?.invoke()
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            _connectionState.value = MoveTransport.ConnectionState.Connecting(endpointId)
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
        val buffer = ByteBuffer.allocate(5)
        buffer.put(1.toByte())
        buffer.putInt(levelNumber)
        sendPayload(buffer.array())
    }

    override fun sendStartGame() {
        val buffer = ByteBuffer.allocate(1)
        buffer.put(3.toByte())
        sendPayload(buffer.array())
    }

    override fun sendRestartGame() {
        val buffer = ByteBuffer.allocate(1)
        buffer.put(4.toByte())
        sendPayload(buffer.array())
    }

    override fun sendQuitGame() {
        val buffer = ByteBuffer.allocate(1)
        buffer.put(5.toByte())
        sendPayload(buffer.array())
    }

    override fun sendMove(type: Int, row: Int, column: Int) {
        val buffer = ByteBuffer.allocate(13)
        buffer.put(2.toByte())
        buffer.putInt(type)
        buffer.putInt(row)
        buffer.putInt(column)
        sendPayload(buffer.array())
    }

    private fun sendPayload(bytes: ByteArray) {
        val endpointId = remoteEndpointId ?: return
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun onLevelReceived(callback: (Int) -> Unit) { levelCallback = callback }
    override fun onStartGameReceived(callback: () -> Unit) { startCallback = callback }
    override fun onRestartGameReceived(callback: () -> Unit) { restartCallback = callback }
    override fun onQuitGameReceived(callback: () -> Unit) { quitCallback = callback }
    override fun onMoveReceived(callback: (Int, Int, Int) -> Unit) { moveCallback = callback }

    override fun startDiscovery() {
        discoveredEndpoints.clear()
        _connectionState.value = MoveTransport.ConnectionState.Discovering
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startDiscovery(serviceId, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                discoveredEndpoints.add(MoveTransport.DiscoveredEndpoint(endpointId, info.endpointName))
                _connectionState.value = MoveTransport.ConnectionState.DiscoveryFound(discoveredEndpoints.toList())
            }

            override fun onEndpointLost(endpointId: String) {
                discoveredEndpoints.removeAll { it.id == endpointId }
                _connectionState.value = MoveTransport.ConnectionState.DiscoveryFound(discoveredEndpoints.toList())
            }
        }, discoveryOptions)
            .addOnFailureListener { e -> _connectionState.value = MoveTransport.ConnectionState.Error("Discovery failed: ${e.message}") }
    }

    override fun connectTo(endpointId: String) {
        _connectionState.value = MoveTransport.ConnectionState.Connecting(endpointId)
        connectionsClient.requestConnection("Player", endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e -> _connectionState.value = MoveTransport.ConnectionState.Error("Connection request failed: ${e.message}") }
    }

    override fun startAdvertising(localName: String) {
        _connectionState.value = MoveTransport.ConnectionState.Advertising
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient.startAdvertising(localName, serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnFailureListener { e -> _connectionState.value = MoveTransport.ConnectionState.Error("Advertising failed: ${e.message}") }
    }

    override fun disconnect() {
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        remoteEndpointId = null
        discoveredEndpoints.clear()
        _connectionState.value = MoveTransport.ConnectionState.Idle
    }
}
