package com.ogonggo.adminapi.ingestion.work24.implement

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/**
 * 고용24에서 받은 이미지를 오공고 저장소에 올릴 크기로 맞춘다.
 *
 * 훈련기관이 올린 사진은 4000픽셀, 3MB짜리 원본이 많아 그대로 쓰면 목록 화면이 무겁다.
 * 긴 변이 [MAX_DIMENSION]을 넘거나 용량이 [MAX_ORIGINAL_BYTES]를 넘으면 줄여서 JPEG로 다시 만든다. 작은 이미지(로고 등)는 그대로 둔다.
 * JPEG와 PNG만 받는다. 읽을 수 없는 이미지는 null이다.
 */
internal object Work24ImageShrinker {

    class Image(val content: ByteArray, val mimeType: String, val extension: String)

    fun shrink(content: ByteArray): Image? {
        val original = when {
            content.startsWith(JPEG_SIGNATURE) -> Image(content, "image/jpeg", "jpg")
            content.startsWith(PNG_SIGNATURE) -> Image(content, "image/png", "png")
            else -> return null
        }
        val (width, height) = dimensions(content) ?: return null
        if (width.toLong() * height > MAX_PIXELS) {
            return null
        }
        val longest = maxOf(width, height)
        if (longest <= MAX_DIMENSION && content.size <= MAX_ORIGINAL_BYTES) {
            return original
        }
        // 다시 만들면 EXIF가 사라져 돌려 찍은 사진이 누운 채로 보인다. 회전값이 있는 사진은 원본을 그대로 쓴다.
        if (isRotated(content)) {
            return original.takeIf { content.size <= MAX_ROTATED_BYTES }
        }

        val source = ImageIO.read(ByteArrayInputStream(content)) ?: return null
        val scale = minOf(1.0, MAX_DIMENSION.toDouble() / longest)
        val resized = resize(source, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        val output = ByteArrayOutputStream()
        check(ImageIO.write(resized, "jpg", output)) { "JPEG로 저장할 수 없는 이미지입니다." }
        return Image(output.toByteArray(), "image/jpeg", "jpg")
    }

    /** 픽셀을 풀지 않고 크기만 읽는다. */
    private fun dimensions(content: ByteArray): Pair<Int, Int>? =
        ImageIO.createImageInputStream(ByteArrayInputStream(content))?.use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.input = input
                reader.getWidth(0) to reader.getHeight(0)
            } finally {
                reader.dispose()
            }
        }

    /**
     * 절반씩 줄여 가며 목표 크기에 맞춘다. 한 번에 크게 줄이면 계단 현상이 생긴다.
     * JPEG는 투명도가 없어 흰 바탕 위에 그린다.
     */
    private fun resize(source: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        var current = source
        var width = source.width
        var height = source.height
        do {
            width = maxOf(targetWidth, width / 2)
            height = maxOf(targetHeight, height / 2)
            val next = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val graphics = next.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, width, height)
                graphics.drawImage(current, 0, 0, width, height, null)
            } finally {
                graphics.dispose()
            }
            current = next
        } while (width > targetWidth || height > targetHeight)
        return current
    }

    /** JPEG의 EXIF 회전값(Orientation, 태그 0x0112)이 1(그대로)이 아닌지 본다. */
    private fun isRotated(content: ByteArray): Boolean {
        if (!content.startsWith(JPEG_SIGNATURE)) {
            return false
        }
        var offset = 2
        while (offset + 4 <= content.size && content[offset] == MARKER) {
            val marker = content[offset + 1].toInt() and 0xFF
            val length = ((content[offset + 2].toInt() and 0xFF) shl 8) or (content[offset + 3].toInt() and 0xFF)
            if (marker == START_OF_SCAN) {
                return false
            }
            if (marker == APP1 && content.startsWith(EXIF_HEADER, offset + 4)) {
                val end = minOf(content.size, offset + 2 + length)
                return orientation(content, offset + 4 + EXIF_HEADER.size, end).let { it != null && it != UPRIGHT }
            }
            offset += 2 + length
        }
        return false
    }

    private fun orientation(content: ByteArray, tiffStart: Int, end: Int): Int? {
        if (tiffStart + 8 > end) {
            return null
        }
        val tiff = ByteBuffer.wrap(content, tiffStart, end - tiffStart).slice()
        tiff.order(if (tiff.get(0) == 'I'.code.toByte()) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        val directory = tiff.getInt(4)
        if (directory < 0 || directory + 2 > tiff.limit()) {
            return null
        }
        val entries = tiff.getShort(directory).toInt() and 0xFFFF
        for (index in 0 until entries) {
            val entry = directory + 2 + index * 12
            if (entry + 12 > tiff.limit()) {
                return null
            }
            if ((tiff.getShort(entry).toInt() and 0xFFFF) == ORIENTATION_TAG) {
                return tiff.getShort(entry + 8).toInt() and 0xFFFF
            }
        }
        return null
    }

    private fun ByteArray.startsWith(prefix: ByteArray, offset: Int = 0): Boolean =
        size >= offset + prefix.size && prefix.indices.all { this[offset + it] == prefix[it] }

    const val MAX_DIMENSION = 1280
    private const val MAX_ORIGINAL_BYTES = 1024 * 1024
    private const val MAX_ROTATED_BYTES = 10 * 1024 * 1024
    private const val MAX_PIXELS = 40_000_000L
    private const val MARKER = 0xFF.toByte()
    private const val APP1 = 0xE1
    private const val START_OF_SCAN = 0xDA
    private const val ORIENTATION_TAG = 0x0112
    private const val UPRIGHT = 1
    private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val EXIF_HEADER = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
}
