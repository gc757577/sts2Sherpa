package com.gchan.sts2_sherpa.util

object KoreanInitialUtils {
    private val initials = charArrayOf(
        'ㄱ',
        'ㄲ',
        'ㄴ',
        'ㄷ',
        'ㄸ',
        'ㄹ',
        'ㅁ',
        'ㅂ',
        'ㅃ',
        'ㅅ',
        'ㅆ',
        'ㅇ',
        'ㅈ',
        'ㅉ',
        'ㅊ',
        'ㅋ',
        'ㅌ',
        'ㅍ',
        'ㅎ',
    )
    private val initialSet = initials.toSet()

    fun getKoreanInitials(text: String): String = buildString {
        text.forEach { char ->
            when {
                char.isHangulSyllable() -> append(initials[(char.code - HANGUL_BASE) / INITIAL_BLOCK_SIZE])
                char.isLetterOrDigit() || char in initialSet -> append(char.lowercaseChar())
            }
        }
    }

    fun normalizeSearchText(text: String): String = buildString {
        text.forEach { char ->
            when {
                char.isLetterOrDigit() || char in initialSet -> append(char.lowercaseChar())
            }
        }
    }

    private fun Char.isHangulSyllable(): Boolean = code in HANGUL_BASE..HANGUL_LAST

    private const val HANGUL_BASE = 0xAC00
    private const val HANGUL_LAST = 0xD7A3
    private const val INITIAL_BLOCK_SIZE = 21 * 28
}
