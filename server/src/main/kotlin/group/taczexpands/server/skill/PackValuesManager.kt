package group.taczexpands.server.skill

import com.google.common.collect.Maps
import com.google.gson.reflect.TypeToken
import com.tacz.guns.resource.CommonAssetsManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.GsonHelper
import net.minecraft.util.profiling.ProfilerFiller

object PackValuesManager : SimplePreparableReloadListener<Map<String, Map<String, Any>>>() {
    private val PACK_VALUES_NAME = "values.json"
    private val dataMap = Maps.newHashMap<String, Map<String, Any>>()
    override fun prepare(manager: ResourceManager, pProfiler: ProfilerFiller): Map<String, Map<String, Any>> {
        val output: MutableMap<String, Map<String, Any>> = Maps.newHashMap()

        for (namespaces in manager.getNamespaces()) {
            manager.getResource(ResourceLocation(namespaces, PACK_VALUES_NAME)).ifPresent { rl: Resource ->
                try {
                    rl.openAsReader().use { reader ->
                        val packValues = GsonHelper.fromJson(
                            CommonAssetsManager.GSON, reader,
                            object : TypeToken<Map<String, Any>>(){}
                        )
                        val packValues1 = output.put(namespaces, packValues)
                        check(packValues1 == null) { "Duplicate data file ignored with namespace $namespaces" }
                    }
                } catch (e: Exception) {
                }

            }
        }
        return output
    }

    override fun apply(p0: Map<String, Map<String, Any>>, p1: ResourceManager, p2: ProfilerFiller) {
        dataMap.clear()
        dataMap.putAll(p0)
    }

    fun getValue(key: String): Any? {
        return dataMap.values.firstOrNull { it.containsKey(key) }?.get(key)
    }
}