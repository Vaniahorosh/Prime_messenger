package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PluginItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var fileType: String = "Java",
    var filePath: String = "",
    var codeContent: String = "",
    var isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object PluginManager {

    private const val PLUGINS_DIR = "plugins"
    private const val METADATA_FILE = "plugins_metadata.json"
    const val ACTION_PLUGINS_CHANGED = "com.messenger.prime.PLUGINS_CHANGED"

    @Synchronized
    fun getAllPlugins(context: Context): MutableList<PluginItem> {
        val dir = File(context.filesDir, PLUGINS_DIR)
        if (!dir.exists()) dir.mkdirs()

        val list = mutableListOf<PluginItem>()
        val metaFile = File(dir, METADATA_FILE)
        if (metaFile.exists()) {
            try {
                val jsonStr = metaFile.readText(Charsets.UTF_8)
                if (jsonStr.isNotBlank()) {
                    val jsonArr = JSONArray(jsonStr)
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        val id = obj.optString("id", UUID.randomUUID().toString())
                        val name = obj.optString("name", "Plugin")
                        val fileType = obj.optString("fileType", "Java")
                        val filePath = obj.optString("filePath", "")
                        var codeContent = obj.optString("codeContent", "")
                        val isEnabled = obj.optBoolean("isEnabled", true)
                        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                        if (codeContent.isBlank() && filePath.isNotBlank()) {
                            try {
                                val f = File(filePath)
                                if (f.exists()) codeContent = f.readText(Charsets.UTF_8)
                            } catch (_: Exception) {}
                        }

                        list.add(
                            PluginItem(
                                id = id,
                                name = name,
                                fileType = fileType,
                                filePath = filePath,
                                codeContent = codeContent,
                                isEnabled = isEnabled,
                                createdAt = createdAt
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Physical file scan fallback
        val files = dir.listFiles() ?: arrayOf()
        for (file in files) {
            if (file.name == METADATA_FILE || file.isDirectory) continue
            val exists = list.any { it.filePath == file.absolutePath || file.name.contains(it.name) }
            if (!exists) {
                try {
                    val code = file.readText(Charsets.UTF_8)
                    val rawName = file.name.substringAfter("_")
                    val ext = when {
                        rawName.endsWith(".java", ignoreCase = true) -> "Java"
                        rawName.endsWith(".kt", ignoreCase = true) -> "Kotlin"
                        rawName.endsWith(".py", ignoreCase = true) -> "Python"
                        rawName.endsWith(".js", ignoreCase = true) -> "JS"
                        rawName.endsWith(".json", ignoreCase = true) -> "JSON"
                        rawName.endsWith(".html", ignoreCase = true) -> "HTML"
                        rawName.endsWith(".sh", ignoreCase = true) -> "Shell"
                        else -> "File"
                    }
                    list.add(
                        PluginItem(
                            id = UUID.randomUUID().toString(),
                            name = rawName,
                            fileType = ext,
                            filePath = file.absolutePath,
                            codeContent = code,
                            isEnabled = true
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        list.sortByDescending { it.createdAt }
        return list
    }

    @Synchronized
    fun savePlugin(context: Context, plugin: PluginItem) {
        val list = getAllPlugins(context)
        val index = list.indexOfFirst { it.id == plugin.id }
        if (index >= 0) {
            list[index] = plugin
        } else {
            list.add(0, plugin)
        }
        saveAllPlugins(context, list)
    }

    @Synchronized
    fun deletePlugin(context: Context, id: String) {
        val list = getAllPlugins(context)
        val item = list.find { it.id == id }
        if (item != null) {
            if (item.filePath.isNotBlank()) {
                try { File(item.filePath).delete() } catch (_: Exception) {}
            }
            list.removeAll { it.id == id }
            saveAllPlugins(context, list)
        }
    }

    @Synchronized
    fun togglePlugin(context: Context, id: String, isEnabled: Boolean) {
        val list = getAllPlugins(context)
        val item = list.find { it.id == id }
        if (item != null) {
            item.isEnabled = isEnabled
            saveAllPlugins(context, list)
        }
    }

    @Synchronized
    fun enableAllPlugins(context: Context) {
        val list = getAllPlugins(context)
        list.forEach { it.isEnabled = true }
        saveAllPlugins(context, list)
    }

    @Synchronized
    fun disableAllPlugins(context: Context) {
        val list = getAllPlugins(context)
        list.forEach { it.isEnabled = false }
        saveAllPlugins(context, list)
    }

    @Synchronized
    fun duplicatePlugin(context: Context, plugin: PluginItem): PluginItem {
        val newName = "Copy_${plugin.name}"
        return createPluginFromCode(context, newName, plugin.codeContent, plugin.fileType)
    }

    @Synchronized
    fun createPluginFromCode(
        context: Context,
        name: String,
        code: String,
        fileType: String = "Java"
    ): PluginItem {
        val dir = File(context.filesDir, PLUGINS_DIR)
        if (!dir.exists()) dir.mkdirs()

        val id = UUID.randomUUID().toString()
        val cleanName = if (name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".json") || name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".html") || name.endsWith(".sh")) name else "$name.${fileType.lowercase()}"
        val codeFile = File(dir, "${id}_$cleanName")
        codeFile.writeText(code, Charsets.UTF_8)

        val plugin = PluginItem(
            id = id,
            name = cleanName,
            fileType = fileType,
            filePath = codeFile.absolutePath,
            codeContent = code,
            isEnabled = true
        )
        savePlugin(context, plugin)
        return plugin
    }

    @Synchronized
    fun importPluginFile(context: Context, uri: Uri, fileName: String?): PluginItem? {
        val dir = File(context.filesDir, PLUGINS_DIR)
        if (!dir.exists()) dir.mkdirs()

        val name = fileName?.ifBlank { "plugin_${System.currentTimeMillis()}" } ?: "plugin_${System.currentTimeMillis()}"
        val ext = when {
            name.endsWith(".java", ignoreCase = true) -> "Java"
            name.endsWith(".kt", ignoreCase = true) -> "Kotlin"
            name.endsWith(".py", ignoreCase = true) -> "Python"
            name.endsWith(".js", ignoreCase = true) -> "JS"
            name.endsWith(".json", ignoreCase = true) -> "JSON"
            name.endsWith(".html", ignoreCase = true) -> "HTML"
            name.endsWith(".sh", ignoreCase = true) -> "Shell"
            else -> "File"
        }

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val content = inputStream.bufferedReader().use { it.readText() }
            return createPluginFromCode(context, name, content, ext)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @Synchronized
    private fun saveAllPlugins(context: Context, list: List<PluginItem>) {
        val dir = File(context.filesDir, PLUGINS_DIR)
        if (!dir.exists()) dir.mkdirs()

        val jsonArr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("fileType", item.fileType)
                put("filePath", item.filePath)
                put("codeContent", item.codeContent)
                put("isEnabled", item.isEnabled)
                put("createdAt", item.createdAt)
            }
            jsonArr.put(obj)
        }

        val metaFile = File(dir, METADATA_FILE)
        metaFile.writeText(jsonArr.toString(), Charsets.UTF_8)

        context.sendBroadcast(Intent(ACTION_PLUGINS_CHANGED).setPackage(context.packageName))
    }

    fun getPluginsSummaryForAi(context: Context): String {
        val enabled = getAllPlugins(context).filter { it.isEnabled }
        if (enabled.isEmpty()) {
            return "Активных плагинов нет."
        }
        val sb = StringBuilder("Установленные и АКТИВНЫЕ плагины Prime Messenger (${enabled.size}):\n")
        enabled.forEachIndexed { idx, p ->
            var content = p.codeContent
            if (content.isBlank() && p.filePath.isNotBlank()) {
                try {
                    val f = File(p.filePath)
                    if (f.exists()) content = f.readText(Charsets.UTF_8)
                } catch (_: Exception) {}
            }
            sb.append("\n=== Плагин ${idx + 1}: ${p.name} [Тип: ${p.fileType}] ===\n")
            if (content.isNotBlank()) {
                sb.append(content)
                sb.append("\n=== Конец плагина ${p.name} ===\n")
            } else {
                sb.append("(Содержимое файла пустое)\n")
            }
        }
        return sb.toString()
    }

    fun runPlugin(context: Context, nameOrId: String, inputArgs: String = ""): String {
        val list = getAllPlugins(context)
        val plugin = list.find { it.id == nameOrId || it.name.equals(nameOrId, ignoreCase = true) || it.name.contains(nameOrId, ignoreCase = true) }
            ?: return "❌ Ошибка: Плагин '$nameOrId' не найден."

        return PluginExecutor.executePlugin(context, plugin, inputArgs)
    }

    fun getSampleJavaTemplate(): String {
        return """
package com.messenger.prime.plugins;

/**
 * Плагин для Prime Messenger (Java)
 */
public class SamplePlugin {

    public String getName() {
        return "Sample Java Extension";
    }

    public void onExecute() {
        // Выполнение функций плагина
        System.out.println("Java Plugin Executed!");
    }
}
        """.trimIndent()
    }

    fun getSampleKotlinTemplate(): String {
        return """
package com.messenger.prime.plugins

/**
 * Плагин для Prime Messenger (Kotlin)
 */
class SampleKotlinPlugin {
    val name: String = "Sample Kotlin Extension"

    fun onExecute() {
        // Выполнение функций плагина
    }
}
        """.trimIndent()
    }
}
