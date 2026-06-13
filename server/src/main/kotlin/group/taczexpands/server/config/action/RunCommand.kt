package group.taczexpands.server.config.action

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
import group.taczexpands.server.expression.ExpressionInstance
import group.taczexpands.server.expression.create
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

@Serializable
@SerialName("RunCommand")
data class RunCommand(
    val command: String? = null,
    val commands: List<String> = listOf(),
    val succeeded: ChainAction? = null,
    val failed: ChainAction? = null,
    val permission: Int? = 4,
    val selector: SelectorData? = null,
    val args: List<ExpressionData>? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = RunCommand("say 1")
    }

    init {
        if (delay != null && delay < 0) throw IllegalArgumentException("RunCommand delay must be greater than 0")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(selector.create(context).toData(), args.create(context).toData())
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val targets = data.dataList[0].selector.getTargets(context)
        val args = data.dataList[1].args
        if (command != null) {
            targets.forEach {
                runCommand(skill, context, command, it, args)
            }
        }
        commands.forEach { command ->
            targets.forEach {
                runCommand(skill, context, command, it, args)
            }
        }
    }

    fun runCommand(skill: Skill, context: Context, command: String, target: Entity, args: List<ExpressionInstance>) {
        val baseCommand = ExpressionHelper.parseNew(command, args, context, target)!!

        val server = context.self.server
        val results = server.commands.dispatcher.parse(
            baseCommand,
            server.createCommandSourceStack().withEntity(target).withSuppressedOutput().also { if (permission != null) it.withPermission(permission) }
        )
        val result = server.commands.performCommand(results, baseCommand)
        if (succeeded != null && result > 0) {
            succeeded.perform(skill, context)
        }
        if (failed != null && result <= 0) {
            failed.perform(skill, context)
        }
    }
}