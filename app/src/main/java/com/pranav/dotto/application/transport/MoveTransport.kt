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
        data object Connecting : ConnectionState
        data class Connected(val remoteEndpointId: String, val remoteName: String) : ConnectionState
        data class Error(val message: String) : ConnectionState
    }

    val connectionState: StateFlow<ConnectionState>

    /** Sends the selected level number to the opponent. */
    fun sendLevel(levelNumber: Int)
    
    /** Sends a move (linear index of the line) to the opponent. */
    fun sendMove(cellIndex: Int)

    /** Called when a level is received from the remote player. */
    fun onLevelReceived(callback: (Int) -> Unit)

    /** Called when a move is received from the remote player. */
    fun onMoveReceived(callback: (Int) -> Unit)

    /** Start looking for opponents (Discovery). */
    fun startDiscovery()

    /** Start being visible to challengers (Advertising). */
    fun startAdvertising(localName: String)

    /** Stop all network activity and disconnect. */
    fun disconnect()
}
