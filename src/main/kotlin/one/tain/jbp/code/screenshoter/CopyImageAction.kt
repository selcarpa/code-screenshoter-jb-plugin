package one.tain.jbp.code.screenshoter

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.Consumer
import one.tain.jbp.code.screenshoter.CopyImageUtils.notificationGroup
import java.awt.Toolkit


class CopyImageAction : BaseImageAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val startTime = System.currentTimeMillis()
        val result = validateAndCreateImage(event) ?: return

        val (project, image, _) = result

        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(image) { _, _ -> }

        notificationGroup
            .createNotification("Image was copied to the clipboard", NotificationType.INFORMATION)
            .setTitle("Code screenshots")
            .addAction(
                DumbAwareAction.create(
                    "Save to File",
                    Consumer { saveImage(image, project) })
            )
            .notify(project)

        val endTime = System.currentTimeMillis()
        Logger.getInstance(CopyImageAction::class.java).info("Copied image in ${endTime - startTime} ms")
    }

    override fun getUnavailableMessage(): String = "'Copy as Image' is available in text editors only"

    override fun getNoSelectionMessage(): String = "Please select the text fragment to copy"

    override fun getWarningMessage(): String = "Copying such a big image could be slow and may take a lot of memory. Proceed?"

    override fun getYesButtonText(): String = "Yes, Copy It!"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
