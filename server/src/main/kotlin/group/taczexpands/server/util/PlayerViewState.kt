package group.taczexpands.server.util

import group.taczexpands.common.network.v2.c2s.C2SViewState

data class PlayerViewState(
    val yaw: Float,
    val pitch: Float,
    val source: C2SViewState.Source,
    val cameraEntityId: Int?,
    val cameraPathSessionId: Long?,
    val clientSequence: Long,
    val lastServerTick: Long
)
