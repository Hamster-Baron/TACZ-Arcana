package group.taczexpands.server.nbt

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.server.ServerLifecycleHooks
import kotlin.collections.component1


abstract class ScoreData<T>(private val storage: DataStorage, val name: String, private val data: MutableMap<String, T>) {
    protected abstract val tagType: Int
    protected abstract fun getDefaultValue(): T
    protected abstract fun get(dataTag: CompoundTag, uuid: String): T
    protected abstract fun set(dataTag: CompoundTag, uuid: String, value: T)

    fun save(): CompoundTag {
        val scoreTag = CompoundTag()
        val dataTag = CompoundTag()

        data.forEach { (uuid, value) ->
            set(dataTag, uuid, value)
        }

        scoreTag.putString("name", name)
        scoreTag.put("data", dataTag)
        return scoreTag
    }

    companion object {
        fun <T, S : ScoreData<T>> load(storage: DataStorage, scoreTag: CompoundTag, constructor: (DataStorage, String, MutableMap<String, T>) -> S): S {
            val dataMap = mutableMapOf<String, T>()

            val name = scoreTag.getString("name")
            val dataTag = scoreTag.getCompound("data")

            val scoreData = constructor(storage, name, dataMap)

            dataTag.allKeys.forEach { uuid ->
                if (dataTag.contains(uuid, scoreData.tagType)) {
                    dataMap[uuid] = scoreData.get(dataTag, uuid)
                }
            }
            return scoreData
        }
    }

    fun hasValue(entity: Entity): Boolean {
        return data.containsKey(entity.stringUUID)
    }

    fun removeValue(entity: Entity): Boolean {
        if (data.containsKey(entity.stringUUID)) {
            data.remove(entity.stringUUID)
            storage.setDirty()
            return true
        }
        return false
    }

    fun getValue(entity: Entity): T {
        return data[entity.stringUUID] ?: getDefaultValue()
    }

    fun setValue(entity: Entity, value: T): Boolean {
        data[entity.stringUUID] = value
        storage.setDirty()
        return true
    }
}

class FloatScoreData(storage: DataStorage, name: String, data: MutableMap<String, Float>) : ScoreData<Float>(storage, name, data) {
    override val tagType: Int = Tag.TAG_FLOAT.toInt()
    override fun getDefaultValue(): Float = 0.0f
    override fun get(dataTag: CompoundTag, uuid: String): Float = dataTag.getFloat(uuid)
    override fun set(dataTag: CompoundTag, uuid: String, value: Float) = dataTag.putFloat(uuid, value)
}

class StringScoreData(storage: DataStorage, name: String, data: MutableMap<String, String>) : ScoreData<String>(storage, name, data) {
    override val tagType: Int = Tag.TAG_STRING.toInt()
    override fun getDefaultValue(): String = ""
    override fun get(dataTag: CompoundTag, uuid: String): String = dataTag.getString(uuid)
    override fun set(dataTag: CompoundTag, uuid: String, value: String) = dataTag.putString(uuid, value)
}


class CompoundTagScoreData(storage: DataStorage, name: String, data: MutableMap<String, CompoundTag>) : ScoreData<CompoundTag>(storage, name, data) {
    override val tagType: Int = Tag.TAG_COMPOUND.toInt()
    override fun getDefaultValue(): CompoundTag = CompoundTag()
    override fun get(dataTag: CompoundTag, uuid: String): CompoundTag = dataTag.getCompound(uuid)
    override fun set(dataTag: CompoundTag, uuid: String, value: CompoundTag) {
        dataTag.put(uuid, value)
    }
}

class DataType<T, S : ScoreData<T>> private constructor(val typeName: String, val constructor: (DataStorage, String, MutableMap<String, T>) -> S) {
    companion object {
        val FLOAT = DataType("floatScores", ::FloatScoreData)
        val STRING = DataType("stringScores", ::StringScoreData)
        val COMPOUND_TAG = DataType("compoundTagScores", ::CompoundTagScoreData)

    }
}

class DataStorage(
) : SavedData() {
    private val allScores: MutableMap<DataType<*, *>, MutableMap<String, ScoreData<*>>> = mutableMapOf()

    companion object {
        private val DATA_TYPE_REGISTRY = mapOf<DataType<*, *>, (dataStorage: DataStorage, scoreTag: CompoundTag) -> ScoreData<*>>(
            DataType.FLOAT to { dataStorage, scoreTag -> ScoreData.load(dataStorage, scoreTag, DataType.FLOAT.constructor) },
            DataType.STRING to { dataStorage, scoreTag -> ScoreData.load(dataStorage, scoreTag, DataType.STRING.constructor) },
            DataType.COMPOUND_TAG to { dataStorage, scoreTag -> ScoreData.load(dataStorage, scoreTag, DataType.COMPOUND_TAG.constructor) }
        )


        fun get(): DataStorage {
            return ServerLifecycleHooks.getCurrentServer().overworld().dataStorage.computeIfAbsent(DataStorage::load, DataStorage::create, "taczexpands")
        }

        private fun create(): DataStorage {
            return DataStorage()
        }

        private fun load(tag: CompoundTag): DataStorage {
            val dataStorage = DataStorage()
            DATA_TYPE_REGISTRY.forEach { (type, loadFunction) ->
                if (tag.contains(type.typeName)) {
                    val listTag = tag.getList(type.typeName, Tag.TAG_COMPOUND.toInt())
                    val categoryMap = mutableMapOf<String, ScoreData<*>>()

                    listTag.forEach { scoreTag ->
                        if (scoreTag is CompoundTag) {
                            val scoreData = loadFunction(dataStorage, scoreTag)
                            categoryMap[scoreData.name] = scoreData
                        }
                    }
                    dataStorage.allScores[type] = categoryMap
                }
            }
            return dataStorage
        }
    }

    override fun save(tag: CompoundTag): CompoundTag {
        allScores.forEach { (type, categoryMap) ->
            val listTag = ListTag()
            categoryMap.values.forEach { scoreData ->
                listTag.add(scoreData.save())
            }
            tag.put(type.typeName, listTag)
        }
        return tag
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, S : ScoreData<T>> getCategory(type: DataType<T, S>): MutableMap<String, S> {
        return allScores.getOrPut(type) { mutableMapOf() } as MutableMap<String, S>
    }

    fun <T, S : ScoreData<T>> hasScore(type: DataType<T, S>, name: String): Boolean {
        return getCategory(type).containsKey(name)
    }

    fun <T, S : ScoreData<T>> getOrCreateScore(type: DataType<T, S>, name: String): S {
        return getScore(type, name) ?: addScore(type, name)!!
    }


    fun <T, S : ScoreData<T>> getScore(type: DataType<T, S>, name: String): S? {
        return getCategory(type)[name]
    }

    fun <T, S : ScoreData<T>> addScore(type: DataType<T, S>, name: String): S? {
        if (hasScore(type, name)) return null
        val scoreData = type.constructor(this, name, mutableMapOf())
        getCategory(type)[name] = scoreData
        setDirty()
        return scoreData
    }

    fun <T, S : ScoreData<T>> removeScore(type: DataType<T, S>, name: String): Boolean {
        if (!hasScore(type, name)) return false
        getCategory(type).remove(name)
        setDirty()
        return true
    }
}

