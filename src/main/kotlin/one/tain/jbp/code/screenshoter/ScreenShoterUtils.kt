package one.tain.jbp.code.screenshoter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.SystemProperties
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths

object ScreenShoterUtils {

    fun pictureDefaultDirectory(): String {
        return picturesDirectory()?.let {
            "$it${File.separator}${CodeScreenshoterBundle.message("plugin.en.name")}${File.separator}"
        } ?: "${SystemProperties.getUserHome()}${File.separator}${CodeScreenshoterBundle.message("plugin.en.name")}${File.separator}"
    }

    fun picturesDirectory(): String? {
        val userHome = System.getProperty("user.home")

        try {
            if (SystemInfo.isWindows) {
                // through Windows's registry
                val process = Runtime.getRuntime().exec(
                    "reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v \"My Pictures\""
                )
                BufferedReader(
                    InputStreamReader(process.inputStream)
                ).use { reader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        if (line!!.contains("My Pictures")) {
                            val parts: Array<String?> =
                                line.trim { it <= ' ' }.split("\\s{4,}".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (parts.size >= 2) {
                                return parts[1]
                            }
                        }
                    }
                }
                return null // 未找到
            } else if (SystemInfo.isMac) {
                // macOS: 通常是 ~/Pictures
                val pictures = Paths.get(userHome, "Pictures")
                return if (Files.exists(pictures)) pictures.toString() else null
            } else {
                // Linux / BSD: 遵循 XDG 规范
                val configPath = Paths.get(userHome, ".config", "user-dirs.dirs")
                if (Files.exists(configPath)) {
                    val lines =
                        Files.readAllLines(configPath)
                    for (line in lines) {
                        if (line.startsWith("XDG_PICTURES_DIR")) {
                            val path = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[1].trim { it <= ' ' }.replace("\"", "")
                            return path.replace($$"$HOME", userHome)
                        }
                    }
                }
                // 如果配置文件不存在，尝试默认 ~/Pictures
                val pictures = Paths.get(userHome, "Pictures")
                return if (Files.exists(pictures)) pictures.toString() else null
            }
        } catch (e: Exception) {
            Logger.getInstance(ScreenShoterUtils::class.java).error(e)
            return null // 出错时返回空字符串
        }
    }
}
