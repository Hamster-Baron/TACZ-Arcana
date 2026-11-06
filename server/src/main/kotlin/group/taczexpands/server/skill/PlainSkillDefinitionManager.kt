package group.taczexpands.server.skill

import group.taczexpands.server.config.ValuesConfig
import group.taczexpands.server.config.VolatileVariablesConfig
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import java.io.File

private data class LocalSkillDefinition(val type: PlainSkillDefinitionType, val source: String, val file: File)

internal object PlainSkillDefinitionManager : SimplePreparableReloadListener<List<PlainSkillDefinition>>() {
    @Volatile
    private var resourceDefinitions: List<PlainSkillDefinition> = emptyList()

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): List<PlainSkillDefinition> {
        val definitions = mutableListOf<PlainSkillDefinition>()
        PlainSkillDefinitionType.entries.forEach { type ->
            manager.listResources("expansions/${type.directory}") { isYamlPath(it.path) }
                .forEach { entry ->
                    val location = entry.key
                    val resource = entry.value
                    val source = "resource:$location"
                    val data = try {
                        resource.open().use { it.readAllBytes() }
                    } catch (e: Exception) {
                        throw IllegalStateException("Failed to read $source", e)
                    }
                    definitions.add(PlainSkillDefinition(type, source, data))
                }
        }
        return definitions.sortedBy { it.source }
    }

    override fun apply(definitions: List<PlainSkillDefinition>, manager: ResourceManager, profiler: ProfilerFiller) {
        resourceDefinitions = definitions.toList()
        SkillReloadCoordinator.requestResourceReload()
    }

    fun load(configPath: File) {
        loadConfiguration(File(configPath, "volatile_variables.yml"), VolatileVariablesConfig::appendData)
        loadConfiguration(File(configPath, "values.yml"), ValuesConfig::appendData)
        discoverLocalDefinitions(configPath).forEach { definition ->
            try {
                SkillManager.appendPlainData(definition.type.dataType, definition.file.readBytes(), SkillDefinitionSourceGroup.LOCAL, definition.source)
            } catch (e: Exception) {
                SkillManager.reportDefinitionError(definition.source, e)
            }
        }
        resourceDefinitions.forEach { definition ->
            SkillManager.appendPlainData(definition.type.dataType, definition.data, SkillDefinitionSourceGroup.RESOURCE_PACK, definition.source)
        }
    }

    private fun discoverLocalDefinitions(configPath: File): List<LocalSkillDefinition> {
        val definitions = mutableListOf<LocalSkillDefinition>()
        val normalizedConfigPath = configPath.toPath().toAbsolutePath().normalize()
        PlainSkillDefinitionType.entries.forEach { type ->
            val directory = File(configPath, type.directory)
            if (!directory.exists()) return@forEach
            if (!directory.isDirectory) {
                SkillManager.reportReloadError("local:${type.directory}", IllegalStateException("Expected a definition directory"))
                return@forEach
            }
            try {
                directory.walkTopDown()
                    .onFail { file, exception -> SkillManager.reportReloadError("local:${type.directory}/${file.name}", exception) }
                    .filter { it.isFile && isYamlPath(it.name) }
                    .forEach { file ->
                        val relativePath = normalizedConfigPath.relativize(file.toPath().toAbsolutePath().normalize()).toString().replace(File.separatorChar, '/')
                        definitions.add(LocalSkillDefinition(type, "local:$relativePath", file))
                    }
            } catch (e: Exception) {
                SkillManager.reportReloadError("local:${type.directory}", e)
            }
        }
        return definitions.sortedBy { it.source }
    }

    private fun loadConfiguration(file: File, append: (ByteArray) -> Unit) {
        if (!file.isFile) return
        try {
            append(file.readBytes())
        } catch (e: Exception) {
            SkillManager.reportDefinitionError("local:${file.name}", e)
        }
    }

    private fun isYamlPath(path: String): Boolean = path.endsWith(".yml") || path.endsWith(".yaml")
}
