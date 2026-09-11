package com.pranav.dotto.application.transport

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for sending/receiving game data over a network.
 * Keeps the game engine decoupled from specific APIs like Nearby Connections.
 */
interface MoveTransport {
    
    sealed interface ConnectionState {
        data object Idle : ConnectionState
        data object Advertising : ConnectionState
        data object Discovering : ConnectionState
        data class DiscoveryFound(val endpoints: List<DiscoveredEndpoint>) : ConnectionState
        data class Connecting(val endpointId: String) : ConnectionState
        data class Connected(val remoteEndpointId: String, val remoteName: String) : ConnectionState
        data class Error(val message: String) : ConnectionState
    }

    data class DiscoveredEndpoint(val id: String, val name: String)

    val connectionState: StateFlow<ConnectionState>

    /** Sends the selected level number to the opponent. */
    fun sendLevel(levelNumber: Int)
    
    /** Sends a start game command. */
    fun sendStartGame()

    /** Sends a restart game command. */
    fun sendRestartGame()

    /** Sends a quit game command. */
    fun sendQuitGame()

    /** Sends a back to room command. */
    fun sendBackToRoom()

    /** Sends a move to the opponent. */
    fun sendMove(type: Int, row: Int, column: Int)

    /** Called when a level is received from the remote player. */
    fun onLevelReceived(callback: (Int) -> Unit)

    /** Called when a start game signal is received. */
    fun onStartGameReceived(callback: () -> Unit)

    /** Called when a restart game signal is received. */
    fun onRestartGameReceived(callback: () -> Unit)

    /** Called when a quit game signal is received. */
    fun onQuitGameReceived(callback: () -> Unit)

    /** Called when a back to room signal is received. */
    fun onBackToRoomReceived(callback: () -> Unit)

    /** Called when a move is received from the remote player. */
    fun onMoveReceived(callback: (Int, Int, Int) -> Unit)

    /** Start looking for opponents (Discovery). */
    fun startDiscovery()

    /** Connect to a discovered opponent. */
    fun connectTo(endpointId: String, localName: String)

    /** Start being visible to challengers (Advertising). */
    fun startAdvertising(localName: String)

    /** Stop all network activity and disconnect. */
    fun disconnect()
}
