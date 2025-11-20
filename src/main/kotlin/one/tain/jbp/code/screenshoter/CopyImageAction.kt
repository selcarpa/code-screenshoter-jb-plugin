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

    override fun getUnavailableMessage(): String = CodeScreenshoterBundle.message("action.copy.as.image.name") +  CodeScreenshoterBundle.message("error.only.editor.support")

    override fun getNoSelectionMessage(): String = CodeScreenshoterBundle.message("message.select.text")

    override fun getWarningMessage(): String = CodeScreenshoterBundle.message("message.large.image.warning")

    override fun getYesButtonText(): String = CodeScreenshoterBundle.message("message.yes.copy")

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
