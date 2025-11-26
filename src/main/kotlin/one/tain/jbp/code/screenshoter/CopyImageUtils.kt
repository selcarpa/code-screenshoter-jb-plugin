package one.tain.jbp.code.screenshoter

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/**
 * Utility object containing common functionality for the code screenshoter plugin.
 * This includes helper methods for accessing editors, creating notifications,
 * and displaying error messages to users.
 */
object CopyImageUtils {
    /** The title to use for plugin notifications in English */
    var notificationTitle: String = CodeScreenshoterBundle.message("plugin.en.name")

    /**
     * Retrieves the editor instance from the given action event context.
     * This method extracts the editor from the action's data context, which is typically
     * available when the action is triggered from within an editor component.
     *
     * @param event The action event containing the editor context
     * @return The Editor instance if available, or null if no editor is associated with the event
     */
    @JvmStatic
    fun getEditor(event: AnActionEvent): Editor? {
        val dataContext = event.dataContext
        return CommonDataKeys.EDITOR.getData(dataContext)
    }

    /**
     * Gets the notification group for the plugin to display user notifications.
     * This group is used to create and manage all notifications shown by the plugin.
     *
     * @return A NotificationGroup instance for the plugin
     */
    @JvmStatic
    val notificationGroup: NotificationGroup
        get() = NotificationGroupManager.getInstance().getNotificationGroup(notificationTitle)

    /**
     * Displays an error notification to the user with the specified error message.
     * This method is used to show error messages to users in a consistent format.
     *
     * @param project The project context for showing the notification, or null if no project is available
     * @param error The error message to display to the user
     */
    @JvmStatic
    fun showError(project: Project?, error: String) {
        notificationGroup
            .createNotification(error, NotificationType.ERROR)
            .setTitle(notificationTitle)
            .notify(project)
    }
}
