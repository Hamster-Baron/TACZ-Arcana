package group.taczexpands.common.nbt

import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.EndTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.ShortTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

sealed class NBTKey<T>(val key: String, val defaultValue: () -> T) {
    abstract val tagType: Int
    abstract fun set(tag: CompoundTag, value: T)
    fun set(itemStack: ItemStack, value: T) {
        set(itemStack.orCreateTag, value)
    }

    protected abstract fun get(tag: CompoundTag?): T
    fun getOrDefault(tag: CompoundTag?): T {
        if (!has(tag)) return defaultValue()
        return get(tag)
    }

    fun getOrDefault(itemStack: ItemStack): T {
        return getOrDefault(itemStack.tag)
    }

    fun getOrSet(tag: CompoundTag, defaultValue: () -> T): T {
        if (!has(tag))
            set(tag, defaultValue())
        return get(tag)
    }

    fun getOrSet(itemStack: ItemStack, default: () -> T): T {
        return getOrSet(itemStack.orCreateTag, default)
    }

    fun has(tag: CompoundTag?): Boolean {
        if (tag == null) return false
        return tag.contains(key, tagType)
    }

    fun has(itemStack: ItemStack): Boolean {
        return has(itemStack.tag)
    }

    fun unset(tag: CompoundTag?) {
        tag?.remove(key)
    }

    fun unset(itemStack: ItemStack) {
        unset(itemStack.tag)
    }
}

fun <T> CompoundTag.set(nbtKey: NBTKey<T>, value: T) {
    nbtKey.set(this, value)
}

fun <T> CompoundTag?.getOrDefault(nbtKey: NBTKey<T>): T {
    return nbtKey.getOrDefault(this)
}

fun <T> CompoundTag?.has(nbtKey: NBTKey<T>): Boolean {
    return nbtKey.has(this)
}

fun <T> CompoundTag?.unset(nbtKey: NBTKey<T>) {
    nbtKey.unset(this)
}

fun <T> CompoundTag.getOrSet(nbtKey: NBTKey<T>, defaultValue: () -> T): T {
    return nbtKey.getOrSet(this, defaultValue)
}

fun <T> CompoundTag.getOrSetDefault(nbtKey: NonNullNBTKey<T>): T {
    return nbtKey.getOrSetDefault(this)
}


fun <T> ItemStack.set(nbtKey: NBTKey<T>, value: T) {
    nbtKey.set(this, value)
}

fun <T> ItemStack.getOrDefault(nbtKey: NBTKey<T>): T {
    return nbtKey.getOrDefault(this)
}

fun <T> ItemStack.has(nbtKey: NBTKey<T>): Boolean {
    return nbtKey.has(this)
}

fun <T> ItemStack.unset(nbtKey: NBTKey<T>) {
    nbtKey.unset(this)
}

fun <T> ItemStack.getOrSet(nbtKey: NBTKey<T>, defaultValue: () -> T): T {
    return nbtKey.getOrSet(this, defaultValue)
}

fun <T> ItemStack.getOrSetDefault(nbtKey: NonNullNBTKey<T>): T {
    return nbtKey.getOrSetDefault(this)
}

abstract class NonNullNBTKey<T>(key: String, defaultValue: () -> T) : NBTKey<T>(key, defaultValue) {
    fun getOrSetDefault(tag: CompoundTag): T {
        if (!has(tag)) set(tag, defaultValue())
        return get(tag)
    }

    fun getOrSetDefault(itemStack: ItemStack): T {
        return getOrSetDefault(itemStack.orCreateTag)
    }
}

abstract class NullableNBTKey<T>(key: String) : NBTKey<T?>(key, { null }) {
}

class FloatKey(key: String, defaultValue: () -> Float) : NonNullNBTKey<Float>(key, defaultValue) {
    override val tagType = Tag.TAG_FLOAT.toInt()

    override fun set(tag: CompoundTag, value: Float) {
        tag.putFloat(key, value)
    }

    override fun get(tag: CompoundTag?): Float {
        return tag!!.getFloat(key)
    }
}

class NullableFloatKey(key: String) : NullableNBTKey<Float>(key) {
    override val tagType = Tag.TAG_FLOAT.toInt()

    override fun set(tag: CompoundTag, value: Float?) {
        if (value != null) tag.putFloat(key, value)
        else tag.remove(key)
    }

    override fun get(tag: CompoundTag?): Float? {
        if (!has(tag)) return null
        return tag!!.getFloat(key)

    }
}

class IntKey(key: String, defaultValue: () -> Int) : NonNullNBTKey<Int>(key, defaultValue) {
    override val tagType = Tag.TAG_INT.toInt()
    override fun set(tag: CompoundTag, value: Int) {
        tag.putInt(key, value)
    }

    override fun get(tag: CompoundTag?): Int {
        return tag!!.getInt(key)
    }
}

class NullableIntKey(key: String) : NullableNBTKey<Int>(key) {
    override val tagType = Tag.TAG_INT.toInt()
    override fun set(tag: CompoundTag, value: Int?) {
        if (value != null) tag.putInt(key, value)
        else tag.remove(key)
    }

    override fun get(tag: CompoundTag?): Int? {
        if (!has(tag)) return null
        return tag!!.getInt(key)
    }
}

class BooleanKey(key: String, defaultValue: () -> Boolean) : NonNullNBTKey<Boolean>(key, defaultValue) {
    override val tagType: Int = Tag.TAG_BYTE.toInt()
    override fun set(tag: CompoundTag, value: Boolean) {
        tag.putBoolean(key, value)
    }

    override fun get(tag: CompoundTag?): Boolean {
        return tag!!.getBoolean(key)
    }
}

class NullableBooleanKey(key: String) : NullableNBTKey<Boolean>(key) {
    override val tagType: Int = Tag.TAG_BYTE.toInt()
    override fun set(tag: CompoundTag, value: Boolean?) {
        if (value != null) tag.putBoolean(key, value)
        else tag.remove(key)
    }

    override fun get(tag: CompoundTag?): Boolean? {
        if (!has(tag)) return null
        return tag!!.getBoolean(key)
    }
}


class StringKey(key: String, defaultValue: () -> String) : NonNullNBTKey<String>(key, defaultValue) {
    override val tagType = Tag.TAG_STRING.toInt()
    override fun set(tag: CompoundTag, value: String) {
        tag.putString(key, value)
    }

    override fun get(tag: CompoundTag?): String {
        return tag!!.getString(key)
    }
}

class NullableStringKey(key: String) : NullableNBTKey<String>(key) {
    override val tagType = Tag.TAG_STRING.toInt()
    override fun set(tag: CompoundTag, value: String?) {
        if (value != null) tag.putString(key, value)
        else tag.remove(key)
    }

    override fun get(tag: CompoundTag?): String? {
        if (!has(tag)) return null
        return tag!!.getString(key)
    }
}

class ResourceLocationKey(key: String, defaultValue: () -> ResourceLocation) : NonNullNBTKey<ResourceLocation>(key, defaultValue) {
    override val tagType = Tag.TAG_STRING.toInt()
    override fun set(tag: CompoundTag, value: ResourceLocation) {
        tag.putString(key, value.toString())
    }

    override fun get(tag: CompoundTag?): ResourceLocation {
        return ResourceLocation(tag!!.getString(key))
    }
}

class NullableResourceLocationKey(key: String) : NullableNBTKey<ResourceLocation>(key) {
    override val tagType = Tag.TAG_STRING.toInt()
    override fun set(tag: CompoundTag, value: ResourceLocation?) {
        if (value != null) tag.putString(key, value.toString())
        else tag.remove(key)

    }

    override fun get(tag: CompoundTag?): ResourceLocation? {
        if (!has(tag)) return null
        return ResourceLocation(tag!!.getString(key))
    }
}

class CompoundTagKey(key: String, defaultValue: () -> CompoundTag) : NonNullNBTKey<CompoundTag>(key, defaultValue) {
    override val tagType = Tag.TAG_COMPOUND.toInt()
    override fun set(tag: CompoundTag, value: CompoundTag) {
        tag.put(key, value)
    }

    override fun get(tag: CompoundTag?): CompoundTag {
        return tag!!.getCompound(key)
    }
}

class NullableCompoundTagKey(key: String) : NullableNBTKey<CompoundTag>(key) {
    override val tagType = Tag.TAG_COMPOUND.toInt()
    override fun set(tag: CompoundTag, value: CompoundTag?) {
        if (value != null) tag.put(key, value)
        else tag.remove(key)
    }

    override fun get(tag: CompoundTag?): CompoundTag? {
        if (!has(tag)) return null
        return tag!!.getCompound(key)
    }
}

fun Tag.unwrap(): Any? {
    return when (this) {
        is ByteTag -> this.asByte
        is ShortTag -> this.asShort
        is IntTag -> this.asInt
        is LongTag -> this.asLong
        is FloatTag -> this.asFloat
        is DoubleTag -> this.asDouble
        is StringTag -> this.asString
        is CompoundTag -> this.allKeys.associateWith { key -> this.get(key)?.unwrap() }
        is ListTag -> this.map { it.unwrap() }
        is ByteArrayTag -> this.asByteArray
        is IntArrayTag -> this.asIntArray
        is LongArrayTag -> this.asLongArray
        is EndTag -> null
        else -> this
    }
}