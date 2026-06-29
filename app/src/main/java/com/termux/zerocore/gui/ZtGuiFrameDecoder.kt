package com.termux.zerocore.gui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build

object ZtGuiFrameDecoder {

    data class Frame(
        val width: Int,
        val height: Int,
        val rgb: ByteArray,
        val bitmap: Bitmap?
    )

    fun decode(bytes: ByteArray): Frame? {
        if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte()) {
            when (bytes[1]) {
                '6'.code.toByte() -> decodePpm6(bytes)?.let { return it }
                '5'.code.toByte() -> decodePpm5(bytes)?.let { return it }
            }
        }
        return decodeCompressed(bytes)
    }

    private fun decodePpm6(bytes: ByteArray): Frame? {
        val header = parsePpmHeader(bytes, binaryBytesPerPixel = 3) ?: return null
        val width = header.width
        val height = header.height
        val expected = width * height * 3
        if (header.dataStart + expected > bytes.size) return null
        val rgb = if (header.maxval == 255) {
            bytes.copyOfRange(header.dataStart, header.dataStart + expected)
        } else {
            scaleRgb(bytes, header.dataStart, width, height, 3, header.maxval)
        }
        return Frame(width, height, rgb, null)
    }

    private fun decodePpm5(bytes: ByteArray): Frame? {
        val header = parsePpmHeader(bytes, binaryBytesPerPixel = 1) ?: return null
        val width = header.width
        val height = header.height
        val expected = width * height
        if (header.dataStart + expected > bytes.size) return null
        val rgb = ByteArray(width * height * 3)
        var dst = 0
        for (j in 0 until expected) {
            val v = scaleSample(bytes[header.dataStart + j], header.maxval)
            rgb[dst++] = v
            rgb[dst++] = v
            rgb[dst++] = v
        }
        return Frame(width, height, rgb, null)
    }

    private data class PpmHeader(
        val width: Int,
        val height: Int,
        val maxval: Int,
        val dataStart: Int
    )

    /** 只在 ASCII 头内解析数字，避免扫进二进制像素（其中可能含 0x0A）。 */
    private fun parsePpmHeader(bytes: ByteArray, binaryBytesPerPixel: Int): PpmHeader? {
        if (bytes.size < 8) return null
        var i = 0
        if (bytes[i] != 'P'.code.toByte()) return null
        i++
        if (bytes[i] != '5'.code.toByte() && bytes[i] != '6'.code.toByte()) return null
        i++
        val numbers = mutableListOf<Int>()
        while (i < bytes.size && numbers.size < 3) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b == '#'.code -> {
                    while (i < bytes.size && bytes[i] != '\n'.code.toByte()) i++
                }
                b.isDigit() -> {
                    var j = i
                    while (j < bytes.size && (bytes[j].toInt() and 0xFF).isDigit()) j++
                    numbers.add(String(bytes, i, j - i).toInt())
                    i = j
                }
                else -> i++
            }
        }
        if (numbers.size < 3) return null
        val width = numbers[0]
        val height = numbers[1]
        val maxval = numbers[2]
        if (width <= 0 || height <= 0 || maxval <= 0) return null
        while (i < bytes.size && bytes[i].toInt().toChar().isWhitespace()) i++
        val expectedBytes = width * height * binaryBytesPerPixel * if (maxval > 255) 2 else 1
        if (i + expectedBytes > bytes.size) return null
        return PpmHeader(width, height, maxval, i)
    }

    private fun scaleRgb(
        bytes: ByteArray,
        start: Int,
        width: Int,
        height: Int,
        channels: Int,
        maxval: Int
    ): ByteArray {
        val count = width * height * channels
        val rgb = ByteArray(count)
        if (maxval == 255) {
            System.arraycopy(bytes, start, rgb, 0, count)
            return rgb
        }
        val bytesPerSample = if (maxval > 255) 2 else 1
        var src = start
        var dst = 0
        repeat(count) {
            rgb[dst++] = if (bytesPerSample == 1) {
                scaleSample(bytes[src++], maxval)
            } else {
                val hi = bytes[src++].toInt() and 0xFF
                val lo = bytes[src++].toInt() and 0xFF
                scaleSample(((hi shl 8) or lo).toByte(), maxval)
            }
        }
        return rgb
    }

    private fun scaleSample(value: Byte, maxval: Int): Byte {
        if (maxval == 255) return value
        val sample = value.toInt() and 0xFF
        return ((sample * 255) / maxval).coerceIn(0, 255).toByte()
    }

    private fun decodeCompressed(bytes: ByteArray): Frame? {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = false
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val rgb = ByteArray(width * height * 3)
        copyToRgb(bitmap, rgb)
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return Frame(width, height, rgb, null)
    }

    private fun copyToRgb(bitmap: Bitmap, rgb: ByteArray) {
        val w = bitmap.width
        val h = bitmap.height
        val row = IntArray(w)
        var dst = 0
        for (y in 0 until h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val px = row[x]
                rgb[dst++] = ((px shr 16) and 0xFF).toByte()
                rgb[dst++] = ((px shr 8) and 0xFF).toByte()
                rgb[dst++] = (px and 0xFF).toByte()
            }
        }
    }

    private fun Int.isDigit(): Boolean = this in '0'.code..'9'.code
}
