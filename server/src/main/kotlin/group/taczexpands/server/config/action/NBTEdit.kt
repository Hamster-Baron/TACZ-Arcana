package group.taczexpands.server.config.action

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.arguments.NbtPathArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.world.entity.LivingEntity


@Serializable
@SerialName("NBTEdit")
data class NBTEdit(
    val action: Action,
    val path: String = "{}",
    val pathIsExpression: Boolean = false,
    val index: Int = 0,
    val data: String = "",
    val dataIsExpression: Boolean = false,
    val unsafeModifySelf: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    @Serializable
    enum class Action {
        @SerialName("insert")
        INSERT,

        @SerialName("set")
        SET,

        @SerialName("merge")
        MERGE,

        @SerialName("remove")
        REMOVE
    }

    companion object {
        val EXAMPLE = NBTEdit(NBTEdit.Action.MERGE, data = """{"example": "data"}""")
        val EXAMPLE2 = NBTEdit(NBTEdit.Action.SET, path = "GunId", data = """"tacz:example_gun"""")
        val EXAMPLE3 = NBTEdit(NBTEdit.Action.REMOVE, path = "AttachmentSCOPE.tag")
        val EXAMPLE4 = NBTEdit(NBTEdit.Action.INSERT, path = "BlockEntityTag.Items", index = 0, data = """{"Slot": 0, "id": "minecraft:diamond", "Count": 1}""")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            val rootTag = if (!unsafeModifySelf) {
                target as? LivingEntity ?: return@forEach
                val itemStack = target.mainHandItem
                itemStack.orCreateTag
            } else {
                target.saveWithoutId(CompoundTag())
            }

            val realPath = if (pathIsExpression) ExpressionHelper.initExpression(path, context, target).evaluate().stringValue else path
            val nbtPath = NbtPathArgument.nbtPath().parse(StringReader(realPath))

            when (action) {
                Action.INSERT -> {
                    val realData = if (dataIsExpression) ExpressionHelper.initExpression(this.data, context, target).evaluate().stringValue else this.data
                    nbtPath.insert(index, rootTag, mutableListOf(TagParser(StringReader(realData)).readValue()))
                }

                Action.SET -> {
                    val realData = if (dataIsExpression) ExpressionHelper.initExpression(this.data, context, target).evaluate().stringValue else this.data
                    nbtPath.set(rootTag, TagParser(StringReader(realData)).readValue())
                }

                Action.MERGE -> {
                    val realData = if (dataIsExpression) ExpressionHelper.initExpression(this.data, context, target).evaluate().stringValue else this.data
                    val toMerge = TagParser(StringReader(realData)).readValue() as CompoundTag
                    nbtPath.getOrCreate(rootTag, ::CompoundTag).forEach {
                        it as CompoundTag
                        it.merge(toMerge)
                    }
                }

                Action.REMOVE -> {
                    nbtPath.remove(rootTag)
                }
            }

            if (unsafeModifySelf) {
                target.load(rootTag)
            }
        }
    }
}