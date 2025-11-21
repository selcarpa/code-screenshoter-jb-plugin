package one.tain.jbp.code.screenshoter

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

object CopyImageUtils {
    var notificationTitle: String = CodeScreenshoterBundle.message("plugin.en.name")

    @JvmStatic
    fun getEditor(event: AnActionEvent): Editor? {
        val dataContext = event.dataContext
        return CommonDataKeys.EDITOR.getData(dataContext)
    }

    @JvmStatic
    val notificationGroup: NotificationGroup
        get() = NotificationGroupManager.getInstance().getNotificationGroup(notificationTitle)

    @JvmStatic
    fun showError(project: Project?, error: String) {
        notificationGroup
            .createNotification(error, NotificationType.ERROR)
            .setTitle(notificationTitle)
            .notify(project)
    }
}
