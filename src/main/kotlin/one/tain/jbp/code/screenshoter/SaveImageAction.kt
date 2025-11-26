package one.tain.jbp.code.screenshoter

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent


/**
 * Action to save selected code as an image file.
 * This class extends BaseImageAction and implements the specific logic for
 * saving code selections as image files to the user-specified directory.
 */
class SaveImageAction : BaseImageAction() {

    /**
     * Performs the main action of saving the selected code as an image file.
     * This method validates the selection, creates the image, and saves it to a file
     * using the user's configured save directory and filename pattern.
     *
     * @param event The action event that triggered this operation
     */
    override fun actionPerformed(event: AnActionEvent) {
        val (project, image, _) =  validateAndCreateImage(event) ?: return

        saveImage(image, project)
    }

    /**
     * Returns the message to show when the action is unavailable (e.g., not in a text editor).
     * This message indicates that the action is only supported in text editors.
     *
     * @return String containing the unavailable message
     */
    override fun getUnavailableMessage(): String = CodeScreenshoterBundle.message("action.save.as.image.name") + " " + CodeScreenshoterBundle.message("error.only.editor.support")

    /**
     * Returns the message to show when no text is selected in the editor.
     * This message prompts the user to select text before performing the save action.
     *
     * @return String containing the no selection message
     */
    override fun getNoSelectionMessage(): String = CodeScreenshoterBundle.message("message.select.text.save")

    /**
     * Returns the warning message shown when attempting to save a large image.
     * This message warns about potential performance issues with large images during save operations.
     *
     * @return String containing the large image save warning message
     */
    override fun getWarningMessage(): String = CodeScreenshoterBundle.message("message.large.image.save.warning")

    /**
     * Returns the text to display on the "Yes" button in confirmation dialogs.
     * This message confirms the user's intent to proceed with saving.
     *
     * @return String containing the yes button text
     */
    override fun getYesButtonText(): String = CodeScreenshoterBundle.message("message.yes.save")

    /**
     * Specifies the thread on which the action updates should be performed.
     * This ensures UI updates happen on the Event Dispatch Thread (EDT).
     *
     * @return ActionUpdateThread.EDT to run updates on the EDT
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
