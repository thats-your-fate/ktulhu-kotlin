package com.ktulhu.ai.util

/**
 * Inserts thin safety spaces between alphabetic/digit boundaries while streaming
 * text chunk by chunk. This mirrors the server-side fixer so partial chunks
 * don't end up gluing characters together on the client.
 */
class BoundaryFixer {
    private var lastChar: Char? = null

    fun apply(chunk: String): String {
        if (chunk.isEmpty()) return chunk

        val capacity = chunk.length + 4
        val out = StringBuilder(capacity)
        for (ch in chunk) {
            val prev = lastChar
            if (prev != null) {
                val prevIsAlpha = prev.isLetter()
                val prevIsDigit = prev.isDigit()
                val chIsAlpha = ch.isLetter()
                val chIsDigit = ch.isDigit()

                if (prevIsAlpha && chIsDigit && !prev.isWhitespace() && !ch.isWhitespace()) {
                    out.append(' ')
                } else if (prevIsDigit && chIsAlpha && !prev.isWhitespace() && !ch.isWhitespace()) {
                    out.append(' ')
                }
            }

            out.append(ch)
            lastChar = ch
        }

        return out.toString()
    }

    fun reset() {
        lastChar = null
    }
}
