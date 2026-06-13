package group.taczexpands.common.manager

import com.google.common.collect.Maps
import com.google.gson.reflect.TypeToken
import com.tacz.guns.resource.CommonAssetsManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.GsonHelper
import net.minecraft.util.profiling.ProfilerFiller

object PackAllowAttachmentsModifyManager : SimplePreparableReloadListener<Map<String, Map<ResourceLocation, List<String>>>>() {
    private val PACK_ALLOW_ATTACHMENTS_NAME = "gunpack_allow_attachments.json"
    private val dataMap = Maps.newHashMap<String, Map<ResourceLocation, List<String>>>()
    override fun prepare(manager: ResourceManager, pProfiler: ProfilerFiller): Map<String, Map<ResourceLocation, List<String>>> {
        val output: MutableMap<String, Map<ResourceLocation, List<String>>> = Maps.newHashMap()

        for (namespaces in manager.getNamespaces()) {
            manager.getResource(ResourceLocation(namespaces, PACK_ALLOW_ATTACHMENTS_NAME)).ifPresent { rl: Resource ->
                try {
                    rl.openAsReader().use { reader ->
                        val packAllowAttachments = GsonHelper.fromJson(
                            CommonAssetsManager.GSON, reader,
                            object : TypeToken<Map<ResourceLocation, List<String>>>() {}
                        )
                        val packAllowAttachments1 = output.put(namespaces, packAllowAttachments)
                        check(packAllowAttachments1 == null) { "Duplicate data file ignored with namespace $namespaces" }
                    }
                } catch (e: Exception) {
                }

            }
        }
        return output
    }

    override fun apply(p0: Map<String, Map<ResourceLocation, List<String>>>, p1: ResourceManager, p2: ProfilerFiller) {
        dataMap.clear()
        dataMap.putAll(p0)
    }

    fun getAllowAttachments(gun: ResourceLocation): List<String> {
        return dataMap.values.mapNotNull {
            it[gun]
        }.flatten().distinct()
    }
}