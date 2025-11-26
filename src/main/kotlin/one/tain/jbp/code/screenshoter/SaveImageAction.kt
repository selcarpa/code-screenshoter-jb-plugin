package one.tain.jbp.code.screenshoter

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent


class SaveImageAction : BaseImageAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val (project, image, _) =  validateAndCreateImage(event) ?: return

        saveImage(image, project)
    }

    override fun getUnavailableMessage(): String = CodeScreenshoterBundle.message("action.save.as.image.name") + " " + CodeScreenshoterBundle.message("error.only.editor.support")

    override fun getNoSelectionMessage(): String = CodeScreenshoterBundle.message("message.select.text.save")

    override fun getWarningMessage(): String = CodeScreenshoterBundle.message("message.large.image.save.warning")

    override fun getYesButtonText(): String = CodeScreenshoterBundle.message("message.yes.save")

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
