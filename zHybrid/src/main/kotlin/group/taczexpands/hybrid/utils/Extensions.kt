package group.taczexpands.hybrid.utils

import com.google.common.hash.Funnels
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import group.taczexpands.common.TACZExpandsCommon
import net.minecraftforge.fml.ModList
import java.io.ByteArrayInputStream
import java.io.File

fun ByteArray.sha256(): String {
    val hasher = Hashing.sha256().newHasher()
    val it = ByteArrayInputStream(this)
    ByteStreams.copy(it, Funnels.asOutputStream(hasher))
    it.close()
    return hasher.hash().toString()

}

fun ByteArray.toIntArray(): IntArray {
    val intArr = IntArray(this.size / 4)
    var offset = 0
    for (i in intArr.indices) {
        intArr[i] = (this[3 + offset].toInt() and 0xFF) or ((this[2 + offset].toInt() and 0xFF) shl 8) or
                ((this[1 + offset].toInt() and 0xFF) shl 16) or ((this[0 + offset].toInt() and 0xFF) shl 24)
        offset += 4
    }
    return intArr
}

object Extensions {
    fun getJarFile(): File? {
        try {
            return ModList.get().getModFileById(TACZExpandsCommon.MODID).file.filePath.toFile()
        } catch (e: Exception) {
            return null
        }
    }

}