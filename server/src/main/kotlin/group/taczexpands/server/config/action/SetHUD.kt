package group.taczexpands.server.config.action

import group.taczexpands.common.data.CoordinateType
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.network.v2.s2c.S2CAddHUD
import group.taczexpands.common.network.v2.s2c.S2CRemoveHUD
import group.taczexpands.common.network.v2.s2c.S2CSendCameraPath
import group.taczexpands.common.util.JSON
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.create
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import java.util.UUID

@Serializable
@SerialName("SetHUD")
data class SetHUD(
    val identifier: String? = null,
    val renderSpace: S2CAddHUD.RenderSpace = S2CAddHUD.RenderSpace.SCREEN,
    val alignment: S2CAddHUD.Alignment = S2CAddHUD.Alignment.CENTER,
    val x: ExpressionData = ExpressionData("0"),
    val y: ExpressionData = ExpressionData("0"),
    val z: ExpressionData = ExpressionData("0"),
    val scale: ExpressionData = ExpressionData("1.0"),
    val text: String? = null,
    val imagePath: String? = null,
    val imageWidth: Int = 256,
    val imageHeight: Int = 256,
    val remove: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = SetHUD()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(selector.create(context).toData(), listOf(x, y, z, scale).create(context).toData())
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { target ->
            if (target !is ServerPlayer) return@forEach
            if (remove) {
                NetworkManager.sendToPlayer(S2CRemoveHUD(identifier).create(), target)
            } else {
                val identifier = identifier ?: UUID.randomUUID().toString()

                val x = data.dataList[1].args[0].get(context, target).numberValue.toDouble()
                val y = data.dataList[1].args[1].get(context, target).numberValue.toDouble()
                val z = data.dataList[1].args[2].get(context, target).numberValue.toDouble()
                val scale = data.dataList[1].args[3].get(context, target).numberValue.toFloat()


                val text = if (text != null) ExpressionHelper.initExpression(text, context, target).evaluate().stringValue else null
                val imagePath = if (imagePath != null) ExpressionHelper.initExpression(imagePath, context, target).evaluate().stringValue else null

                NetworkManager.sendToPlayer(S2CAddHUD(identifier, renderSpace, alignment, x, y, z, scale, text, imagePath, imageWidth, imageHeight).create(), target)
            }
        }
    }
}