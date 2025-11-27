package one.tain.jbp.code.screenshoter

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.SystemProperties
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Utility object containing common functionality for the code screenshoter plugin.
 */
object ScreenShoterUtils {

    /** The title to use for plugin notifications in English */
    var notificationTitle: String = CodeScreenshoterBundle.message("plugin.en.name")

    /**
     * Gets the notification group for the plugin to display user notifications.
     * This group is used to create and manage all notifications shown by the plugin.
     *
     * @return A NotificationGroup instance for the plugin
     */
    val notificationGroup: NotificationGroup
        get() = NotificationGroupManager.getInstance().getNotificationGroup(notificationTitle)

    /**
     * Displays an error notification to the user with the specified error message.
     * This method is used to show error messages to users in a consistent format.
     *
     * @param project The project context for showing the notification, or null if no project is available
     * @param error The error message to display to the user
     */
    fun showError(project: Project?, error: String) {
        notificationGroup
            .createNotification(error, NotificationType.ERROR)
            .setTitle(notificationTitle)
            .notify(project)
    }
    /**
     * Gets the default directory for saving screenshots.
     * This method attempts to find the user's Pictures directory and creates a subdirectory
     * for the plugin. If the Pictures directory cannot be found, it falls back to the
     * user's home directory.
     *
     * @return A string representing the default directory path for saving screenshots
     */
    fun pictureDefaultDirectory(): String {
        return picturesDirectory()?.let {
            "$it${File.separator}${CodeScreenshoterBundle.message("plugin.en.name")}${File.separator}"
        } ?: "${SystemProperties.getUserHome()}${File.separator}${CodeScreenshoterBundle.message("plugin.en.name")}${File.separator}"
    }

    /**
     * Determines the system-specific Pictures directory path.
     * This method handles different operating systems (Windows, macOS, Linux/BSD) to
     * find the appropriate user Pictures directory according to system conventions.
     *
     * @return A string representing the Pictures directory path, or null if it cannot be determined
     */
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
