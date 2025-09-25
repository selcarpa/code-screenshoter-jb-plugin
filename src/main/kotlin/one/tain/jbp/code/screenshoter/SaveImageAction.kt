package one.tain.jbp.code.screenshoter

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent


class SaveImageAction : BaseImageAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val result = validateAndCreateImage(event) ?: return

        val (project, image, _) = result
        saveImage(image, project)
    }

    override fun getUnavailableMessage(): String = "'Save as Image' is available in text editors only"

    override fun getNoSelectionMessage(): String = "Please select the text fragment to save"

    override fun getWarningMessage(): String = "Saving such a big image could be slow and may take a lot of memory. Proceed?"

    override fun getYesButtonText(): String = "Yes, Save It!"

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
