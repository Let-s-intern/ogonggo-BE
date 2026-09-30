package com.ogonggo.adminapi.ingestion.work24.implement

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class Work24ImageShrinkerTest {

    @Test
    fun `큰 사진은 긴 변을 1280픽셀로 줄여 JPEG로 다시 만든다`() {
        val shrunk = requireNotNull(Work24ImageShrinker.shrink(work24TestImage(3000, 2000, "png")))

        val image = ImageIO.read(ByteArrayInputStream(shrunk.content))
        assertEquals(1280, image.width)
        assertEquals(853, image.height)
        assertEquals("image/jpeg", shrunk.mimeType)
        assertEquals("jpg", shrunk.extension)
    }

    @Test
    fun `로고처럼 작은 이미지는 그대로 둔다`() {
        val logo = work24TestImage(152, 90, "png")

        val shrunk = requireNotNull(Work24ImageShrinker.shrink(logo))

        assertArrayEquals(logo, shrunk.content)
        assertEquals("image/png", shrunk.mimeType)
    }

    @Test
    fun `돌려 찍은 사진은 다시 만들면 누워 보이므로 원본을 그대로 쓴다`() {
        val rotated = withExifOrientation(work24TestImage(3000, 2000, "jpg"), orientation = 6)

        val shrunk = requireNotNull(Work24ImageShrinker.shrink(rotated))

        assertArrayEquals(rotated, shrunk.content)
    }

    @Test
    fun `JPEG나 PNG가 아니면 받지 않는다`() {
        assertNull(Work24ImageShrinker.shrink("<script>alert('오류');</script>".toByteArray()))
        assertTrue(Work24ImageShrinker.shrink(ByteArray(0)) == null)
    }

    /** JPEG 시작 표시(SOI) 바로 뒤에 회전값만 담은 EXIF(APP1) 구간을 끼워 넣는다. */
    private fun withExifOrientation(jpeg: ByteArray, orientation: Int): ByteArray {
        val tiff = byteArrayOf(
            0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08, // 빅 엔디언 TIFF 머리말, 첫 디렉터리 위치 8
            0x00, 0x01, // 항목 1개
            0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, orientation.toByte(), 0x00, 0x00, // Orientation
            0x00, 0x00, 0x00, 0x00, // 다음 디렉터리 없음
        )
        val payload = "Exif".toByteArray() + byteArrayOf(0, 0) + tiff
        val length = payload.size + 2
        val segment = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (length shr 8).toByte(), length.toByte()) + payload
        return jpeg.copyOfRange(0, 2) + segment + jpeg.copyOfRange(2, jpeg.size)
    }
}

/** 잡음이 섞인 테스트 이미지다. 단색이면 너무 잘 압축되어 용량 기준을 확인할 수 없다. */
internal fun work24TestImage(width: Int, height: Int, format: String): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val random = java.util.Random(7)
    for (y in 0 until height) {
        for (x in 0 until width) {
            image.setRGB(x, y, random.nextInt(0xFFFFFF))
        }
    }
    return ByteArrayOutputStream().also { ImageIO.write(image, format, it) }.toByteArray()
}
