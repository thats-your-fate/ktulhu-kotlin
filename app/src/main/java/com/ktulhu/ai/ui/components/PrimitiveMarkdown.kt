package com.ktulhu.ai.ui.components.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlin.text.append
import androidx.compose.ui.unit.sp


/**
 * Lightweight Markdown → AnnotatedString parser for chat responses.
 * Supports:
 * - **bold**
 * - *italic*
 * - `inline code`
 * - ``` code blocks ```
 * - bullet lists (-, *, +)
 * - numbered lists (1.)
 * - paragraphs + newlines
 */
object PrimitiveMarkdown {




    fun parse(md: String, textColor: androidx.compose.ui.graphics.Color): AnnotatedString {
        val builder = AnnotatedString.Builder()

        var i = 0
        val length = md.length

        fun peek(n: Int = 0): Char? =
            if (i + n < length) md[i + n] else null

        fun consume(count: Int = 1) { i += count }

        fun append(s: String) { builder.append(s) }

        fun styled(text: String, style: SpanStyle) {
            builder.withStyle(style) { append(text) }
        }

        fun appendAnnotated(astring: AnnotatedString) {
            builder.append(astring)
        }

        while (i < length) {

            // ======== HEADINGS ========
            if (md.startsWith("### ", i)) {
                consume(4)
                val start = i
                val end = md.indexOf("\n", start).takeIf { it != -1 } ?: length
                val text = md.substring(start, end)
                consume(text.length)

                styled(text + "\n", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                continue
            }

            if (md.startsWith("## ", i)) {
                consume(3)
                val start = i
                val end = md.indexOf("\n", start).takeIf { it != -1 } ?: length
                val text = md.substring(start, end)
                consume(text.length)

                styled(text + "\n", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                continue
            }

            if (md.startsWith("# ", i)) {
                consume(2)
                val start = i
                val end = md.indexOf("\n", start).takeIf { it != -1 } ?: length
                val text = md.substring(start, end)
                consume(text.length)

                styled(text + "\n", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp))
                continue
            }

            // ======== CODE BLOCKS ========
            if (md.startsWith("```", i)) {
                consume(3)
                val start = i
                val end = md.indexOf("```", start).takeIf { it != -1 } ?: length
                val code = md.substring(start, end)
                consume(code.length + 3)

                styled(
                    code.trimEnd() + "\n",
                    SpanStyle(
                        background = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                        color = androidx.compose.ui.graphics.Color(0xFFEDEDED),
                        fontFamily = FontFamily.Monospace
                    )
                )
                continue
            }

            // ======== INLINE CODE ========
            if (peek() == '`') {
                consume()
                val start = i
                val end = md.indexOf("`", start).takeIf { it != -1 } ?: length
                val code = md.substring(start, end)
                consume(code.length + 1)

                styled(
                    code,
                    SpanStyle(
                        background = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
                        fontFamily = FontFamily.Monospace
                    )
                )
                continue
            }

            // ======== BOLD ========
            if (md.startsWith("**", i)) {
                consume(2)
                val start = i
                val end = md.indexOf("**", start).takeIf { it != -1 } ?: length
                val boldText = md.substring(start, end)
                consume(boldText.length + 2)

                styled(boldText, SpanStyle(fontWeight = FontWeight.Bold))
                continue
            }

            // ======== ITALIC ========
            if (peek() == '*' && peek(1) != '*') {
                consume()
                val start = i
                val end = md.indexOf("*", start).takeIf { it != -1 } ?: length
                val italic = md.substring(start, end)
                consume(italic.length + 1)

                styled(italic, SpanStyle(fontStyle = FontStyle.Italic))
                continue
            }

            // ======== BULLET LISTS ========
            // "-", "*", "+", "•"
// ======== BULLET LISTS ========
            val bulletChars = listOf('-', '*', '+', '•')
            if (peek() in bulletChars && peek(1) == ' ') {
                consume(2)
                val start = i
                val end = md.indexOf("\n", start).takeIf { it != -1 } ?: length
                val content = md.substring(start, end)
                consume(content.length)

                append("• ")
                appendAnnotated(parse(content, textColor))
                append("\n")
                continue
            }


            // ======== ORDERED LIST ========
// ======== ORDERED LIST ========
            if (peek()?.isDigit() == true && peek(1) == '.' && peek(2) == ' ') {
                val number = peek()!!
                consume(3)

                val start = i
                val end = md.indexOf("\n", start).takeIf { it != -1 } ?: length
                val item = md.substring(start, end)
                consume(item.length)

                append("$number. ")
                appendAnnotated(parse(item, textColor))   // ✅ correct
                append("\n")
                continue
            }


            // ======== DEFAULT ========
            append(peek().toString())
            consume()
        }

        return builder.toAnnotatedString()
    }
}
