package com.messenger.prime

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView

object RichAiMessageParser {

    fun extractImageUrls(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()
        val list = mutableListOf<String>()
        val imageRegex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
        imageRegex.findAll(rawText).forEach { match ->
            val url = match.groupValues[2].trim()
            if (url.isNotBlank() && !list.contains(url)) {
                list.add(url)
            }
        }
        val imgTagRegex = Regex("<img[^>]+src=[\"']([^\"']+)[\"']")
        imgTagRegex.findAll(rawText).forEach { match ->
            val url = match.groupValues[1].trim()
            if (url.isNotBlank() && !list.contains(url)) {
                list.add(url)
            }
        }
        return list
    }

    fun formatAndApply(textView: TextView, rawText: String) {
        if (rawText.isBlank()) {
            textView.text = ""
            return
        }

        var text = rawText

        // 1. Convert Markdown Tables to clean HTML <table>
        val tableRegex = Regex("((?:\\|[^\\n]+\\|\\r?\\n)+)")
        text = tableRegex.replace(text) { match ->
            val lines = match.value.trim().split("\n")
            val sb = StringBuilder("<br/><table border='1' style='border-collapse:collapse; width:100%; margin:8px 0; background-color:#1E293B; border-color:#334155;'>")
            var isHeader = true
            for (line in lines) {
                if (line.contains("---")) continue
                val cells = line.split("|").filter { it.trim().isNotEmpty() }
                if (cells.isNotEmpty()) {
                    sb.append("<tr>")
                    for (cell in cells) {
                        val tag = if (isHeader) "th" else "td"
                        val bg = if (isHeader) "background-color:#0F172A; color:#38BDF8; font-weight:bold;" else "color:#F8FAFC;"
                        sb.append("<$tag style='padding:8px; border:1px solid #334155; $bg'>").append(cell.trim()).append("</$tag>")
                    }
                    sb.append("</tr>")
                    isHeader = false
                }
            }
            sb.append("</table><br/>")
            sb.toString()
        }

        // 2. Convert Markdown Images ![alt](url) -> <img src="url"/>
        val imageRegex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
        text = imageRegex.replace(text) { match ->
            val url = match.groupValues[2]
            "<br/><img src='$url'/><br/>"
        }

        // 3. Convert Markdown Links [text](url) -> <a href="url">text</a>
        val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
        text = linkRegex.replace(text) { match ->
            val label = match.groupValues[1]
            val url = match.groupValues[2]
            "<a href='$url'>$label</a>"
        }

        // 4. Convert Markdown Bold **text** -> <b>text</b>
        text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")

        // 5. Convert Markdown Italic *text* -> <i>text</i>
        text = text.replace(Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)"), "<i>$1</i>")

        // 6. Convert Code Blocks ```code``` -> <pre><code>code</code></pre>
        val codeBlockRegex = Regex("```(?:[a-zA-Z]+)?\\s*\\n([\\s\\S]*?)```")
        text = codeBlockRegex.replace(text) { match ->
            val code = match.groupValues[1].replace("<", "&lt;").replace(">", "&gt;")
            "<br/><pre style='background:#1E293B; color:#38BDF8; padding:10px; border-radius:8px;'><code>$code</code></pre><br/>"
        }

        // 7. Convert Line Breaks
        text = text.replace("\n", "<br/>")

        val imageGetter = RichTextUtils.GlideImageGetter(textView)
        val formatted = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
        textView.text = formatted
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}
