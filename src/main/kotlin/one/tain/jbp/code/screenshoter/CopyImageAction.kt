package one.tain.jbp.code.screenshoter

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.Consumer
import one.tain.jbp.code.screenshoter.CopyImageUtils.notificationGroup
import java.awt.Toolkit


/**
 * Action to copy selected code as an image to the system clipboard.
 * This class extends BaseImageAction and implements the specific logic for
 * copying code selections as images to the clipboard, with performance logging
 * and user notifications.
 */
class CopyImageAction : BaseImageAction() {

    /**
     * Performs the main action of copying the selected code as an image.
     * This method validates the selection, creates the image, copies it to the clipboard,
     * shows a notification, and logs performance metrics.
     *
     * @param event The action event that triggered this operation
     */
    override fun actionPerformed(event: AnActionEvent) {
        val startTime = System.currentTimeMillis()
        val (project, image, _)  = validateAndCreateImage(event) ?: return

        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(image) { _, _ -> }

        notificationGroup
            .createNotification(CodeScreenshoterBundle.message("notification.image.copied"), NotificationType.INFORMATION)
            .setTitle(CodeScreenshoterBundle.message("notification.title"))
            .addAction(
                DumbAwareAction.create(
                    CodeScreenshoterBundle.message("action.save.to.file.name"),
                    Consumer { saveImage(image, project) })
            )
            .notify(project)

        val endTime = System.currentTimeMillis()
        Logger.getInstance(CopyImageAction::class.java).info("Copied image in ${endTime - startTime} ms")
    }

    /**
     * Returns the message to show when the action is unavailable (e.g., not in a text editor).
     * This message indicates that the action is only supported in text editors.
     *
     * @return String containing the unavailable message
     */
    override fun getUnavailableMessage(): String = CodeScreenshoterBundle.message("action.copy.as.image.name") + " " + CodeScreenshoterBundle.message("error.only.editor.support")

    /**
     * Returns the message to show when no text is selected in the editor.
     * This message prompts the user to select text before performing the action.
     *
     * @return String containing the no selection message
     */
    override fun getNoSelectionMessage(): String = CodeScreenshoterBundle.message("message.select.text")

    /**
     * Returns the warning message shown when attempting to create a large image.
     * This message warns about potential performance issues with large images.
     *
     * @return String containing the large image warning message
     */
    override fun getWarningMessage(): String = CodeScreenshoterBundle.message("message.large.image.warning")

    /**
     * Returns the text to display on the "Yes" button in confirmation dialogs.
     * This message confirms the user's intent to proceed with copying.
     *
     * @return String containing the yes button text
     */
    override fun getYesButtonText(): String = CodeScreenshoterBundle.message("message.yes.copy")

    /**
     * Specifies the thread on which the action updates should be performed.
     * This ensures UI updates happen on the Event Dispatch Thread (EDT).
     *
     * @return ActionUpdateThread.EDT to run updates on the EDT
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
