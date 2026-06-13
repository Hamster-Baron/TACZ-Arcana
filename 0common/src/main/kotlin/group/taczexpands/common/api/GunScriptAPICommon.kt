package group.taczexpands.common.api

import net.minecraft.world.entity.LivingEntity

object GunScriptAPICommon {
    var getPlayerScoreIntServerDelegate: ((LivingEntity, String) -> Int)? = null
    var getPlayerScoreFloatServerDelegate: ((LivingEntity, String) -> Float)? = null
    var getPlayerScoreStringServerDelegate: ((LivingEntity, String) -> String)? = null

    var getPlayerScoreIntClientDelegate: ((LivingEntity, String) -> Int)? = null
    var getPlayerScoreFloatClientDelegate: ((LivingEntity, String) -> Float)? = null
    var getPlayerScoreStringClientDelegate: ((LivingEntity, String) -> String)? = null

    var setPlayerScoreIntDelegate: ((LivingEntity, String, Int) -> Boolean)? = null
    var setPlayerScoreFloatDelegate: ((LivingEntity, String, Float) -> Boolean)? = null
    var setPlayerScoreStringDelegate: ((LivingEntity, String, String) -> Boolean)? = null

    var dispatchSignalDelegate: ((LivingEntity, String, Int) -> Boolean)? = null

    var parseExpressionDelegate: ((LivingEntity, String) -> String)? = null
    var refreshVariableDelegate: ((LivingEntity, String) -> Unit)? = null

    var refreshAllVariableDelegate: ((LivingEntity) -> Unit)? = null

    var modifyDelegate: ((LivingEntity, String, String, String) -> Unit)? = null

    var storeLockingTarget: ((LivingEntity) -> Unit)? = null
}