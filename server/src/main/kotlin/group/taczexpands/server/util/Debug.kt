package group.taczexpands.server.util

import group.taczexpands.server.config.action.*
import group.taczexpands.server.config.condition.*
import group.taczexpands.server.skill.TriggerType
import java.io.File

object Debug {
    fun dump() {
        val allTriggers = TriggerType.entries
        val allBuiltinVariables = group.taczexpands.server.expression.Variables.builtinVars.values
        val allModifiableVariables = PARAMETERS
        File("allTriggers.txt").writeText(allTriggers.joinToString(System.lineSeparator()) { "${it.name}(${it.reversible})" })
        File("allBuiltinVariables.txt").writeText(allBuiltinVariables.filter { it.name != null }.joinToString(System.lineSeparator()) { "${it.name!!}" })
        File("allModifiableVariables.txt").writeText(allModifiableVariables.joinToString(System.lineSeparator()) { it })
    }


    val CONDITIONS = listOf(
        TargetIsPlayer.EXAMPLE,
        TargetIsEntities.EXAMPLE,
        SelfStandsOn.EXAMPLE,
        HitBlockType.EXAMPLE,
        Weather.EXAMPLE,
        Biome.EXAMPLE,
        Time.EXAMPLE,
        HasItem.EXAMPLE,
        HasEffect.EXAMPLE,
        ConsumeItem.EXAMPLE,
        BulletFlyingTime.EXAMPLE,
        Variables.EXAMPLE,
        AmmoType.EXAMPLE,
        IsBraced.EXAMPLE,
        SignalState.EXAMPLE,
        WorldType.EXAMPLE,
        HasAttachment.EXAMPLE,
        EntityInRange.EXAMPLE,
        DamageType.EXAMPLE,
        HasPermission.EXAMPLE,
        ChainCondition.EXAMPLE,
        ArmorType.EXAMPLE,
        AmmoCount.EXAMPLE,
        HasMod.EXAMPLE,
        IsChangeAttachment.EXAMPLE,
        HasNBT.EXAMPLE
    )

    val ACTIONS = listOf(
        RunCommand.EXAMPLE,
        Modify.EXAMPLE,
        Animation.EXAMPLE,
        ParticleEmitter.EXAMPLE,
        Summon.EXAMPLE,
        Effect.EXAMPLE,
        FlashEmitter.EXAMPLE,
        SudoAction.EXAMPLE,
        CancelAction.EXAMPLE,
        CancelSkill.EXAMPLE,
        RenderUtil.EXAMPLE,
        InvalidateCache.EXAMPLE,
        CustomDisplay.EXAMPLE,
        Sound.EXAMPLE,
        Message.EXAMPLE,
        Timer.EXAMPLE,
        TriggerSignal.EXAMPLE,
        SetLockingTarget.EXAMPLE,
        SetLockingPos.EXAMPLE,
        Hurt.EXAMPLE,
        Ignite.EXAMPLE,
        Frostbite.EXAMPLE,
        ChainAction.EXAMPLE,
        Branch.EXAMPLE,
        ForceFire.EXAMPLE,
        Shake.EXAMPLE,
        ForceRotation.EXAMPLE,
        SetAmmoType.EXAMPLE,
        GenerateBullet.EXAMPLE,
        MythicMobsSkill.EXAMPLE,
        SetAttachment.EXAMPLE,
        SetTexture.EXAMPLE,
        SetCamera.EXAMPLE,
        NBTEdit.EXAMPLE,
        NBTEdit.EXAMPLE2,
        NBTEdit.EXAMPLE3,
        NBTEdit.EXAMPLE4,
        Dash.EXAMPLE,
        SetHUD.EXAMPLE,
        DisableItem.EXAMPLE
    )
}