@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.tuils

import android.app.ActivityManager
import android.os.Environment
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

object SystemUtils {
    const val TERA: Int = 0
    const val GIGA: Int = 1
    const val MEGA: Int = 2
    const val KILO: Int = 3
    const val BYTE: Int = 4

    private var totalRam = -1L

    @JvmStatic
    fun freeRam(activityManager: ActivityManager, memory: ActivityManager.MemoryInfo): Double {
        activityManager.getMemoryInfo(memory)
        return memory.availMem.toDouble()
    }

    @JvmStatic
    fun totalRam(): Long {
        if (totalRam != -1L) {
            return totalRam
        }

        try {
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            var line = reader.readLine()
            while (line != null) {
                if (line.contains("MemTotal")) {
                    val split = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    totalRam = split[1].toLong()
                    return totalRam
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return 0
    }

    @JvmStatic
    fun getAvailableInternalMemorySize(unit: Int): Double {
        val path = Environment.getDataDirectory()
        return getAvailableSpace(path, unit)
    }

    @JvmStatic
    fun getTotalInternalMemorySize(unit: Int): Double {
        val path = Environment.getDataDirectory()
        return getTotalSpace(path, unit)
    }

    @JvmStatic
    fun getAvailableExternalMemorySize(unit: Int): Double {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val path = Environment.getExternalStorageDirectory()
            getAvailableSpace(path, unit)
        } else {
            0.0
        }
    }

    @JvmStatic
    fun getTotalExternalMemorySize(unit: Int): Double {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val path = Environment.getExternalStorageDirectory()
            getTotalSpace(path, unit)
        } else {
            0.0
        }
    }

    @JvmStatic
    fun getAvailableSpace(path: File, unit: Int): Double {
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        return formatSize(availableBlocks * blockSize, unit)
    }

    @JvmStatic
    fun getTotalSpace(path: File, unit: Int): Double {
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        return formatSize(totalBlocks * blockSize, unit)
    }

    @JvmStatic
    fun formatSize(size: Long, unit: Int): Double {
        val bytes = size.toDouble()
        val result = when (unit) {
            TERA -> bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0
            GIGA -> bytes / 1024.0 / 1024.0 / 1024.0
            MEGA -> bytes / 1024.0 / 1024.0
            KILO -> bytes / 1024.0
            BYTE -> bytes
            else -> return -1.0
        }
        return round(result, 2)
    }

    @JvmStatic
    fun percentage(v: Double, total: Double): Double {
        if (total <= 0) {
            return 0.0
        }
        return round(v * 100.0 / total, 2)
    }

    @JvmStatic
    fun round(d: Double, decimalPlace: Int): Double {
        val power = Math.pow(10.0, decimalPlace.toDouble())
        return Math.round(d * power) / power
    }
}
