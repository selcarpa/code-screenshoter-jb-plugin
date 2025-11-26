package one.tain.jbp.code.screenshoter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import one.tain.jbp.code.screenshoter.CopyImageOptionsProvider.Companion.getInstance
import one.tain.jbp.code.screenshoter.ScreenShoterUtils.getEditor
import one.tain.jbp.code.screenshoter.ScreenShoterUtils.notificationGroup
import one.tain.jbp.code.screenshoter.ScreenShoterUtils.showError
import java.awt.Desktop
import java.awt.geom.Rectangle2D
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Abstract base class for image-related actions in the code screenshoter plugin.
 * This class provides common functionality for copying and saving code selections as images,
 * including validation, image creation, and file saving operations.
 */
abstract class BaseImageAction : AnAction() {

    /**
     * Validates the current editor state and creates an image from the selected code.
     * This method checks if there's a valid editor and selection, warns about large images,
     * and returns a triple containing the project, the created image, and the selection rectangle.
     *
     * @param event The action event that triggered this operation
     * @return A Triple containing the project, the created TransferableImage, and the selection rectangle, or null if validation fails
     */
    protected fun validateAndCreateImage(event: AnActionEvent): Triple<Project, TransferableImage<*>, Rectangle2D>? {
        val project = event.project ?: return null
        val editor = getEditor(event) ?: run {
            showError(project, getUnavailableMessage())
            return null
        }

        if (!editor.selectionModel.hasSelection()) {
            showError(project, getNoSelectionMessage())
            return null
        }

        val lineCount = editor.document.lineCount

        //todo: not only line count is limit for image creation, row width is also limit. there should be a line count check with row width check

        // constructing a huge image, promote user to confirm
        if (lineCount > getInstance(project).state.lineLimitToWarn) {
            if (Messages.showYesNoDialog(
                    project,
                    getWarningMessage(getInstance(project).state.lineLimitToWarn.toString()),
                    CodeScreenshoterBundle.message("plugin.name"),
                    getYesButtonText(),
                    CodeScreenshoterBundle.message("message.cancel"),
                    null
                ) != Messages.YES
            ) {
                return null
            }
        }
        val imageBuilder = ImageBuilder(editor)
        val rectangle = imageBuilder.selectedSize()

        val image = imageBuilder.createImage(rectangle) ?: return null

        return Triple(project, image, rectangle)
    }

    /**
     * Saves the given image to the user-specified directory with a timestamped filename.
     * This method handles file creation, image writing, and displays notifications about the save operation.
     * It also provides an option to open the saved file in the system's default application.
     *
     * @param image The TransferableImage to save
     * @param project The project context for accessing user settings
     */
    protected fun saveImage(image: TransferableImage<*>, project: Project) {
        val options = getInstance(project).state
        var toSave = options.directoryToSave
        if (StringUtil.isEmpty(toSave)) {
            toSave = ScreenShoterUtils.pictureDefaultDirectory()
        }
        toSave = toSave.trim { it <= ' ' }
        val now = LocalDateTime.now()
        val date = DateTimeFormatter.ofPattern(getInstance(project).state.dateTimePattern).format(now)
        val fileName = "Shot_" + date + "." + image.format.ext
        val path = Paths.get(FileUtil.toSystemDependentName(toSave), fileName)
        try {
            Files.createDirectories(path.parent)

            Files.newOutputStream(path).use { os ->
                image.write(os)
            }
            val pathRepresentation =
                StringUtil.escapeXmlEntities(StringUtil.shortenPathWithEllipsis(path.toString(), 50))
            val notification = notificationGroup
                .createNotification(pathRepresentation, NotificationType.INFORMATION)
                .setTitle(CodeScreenshoterBundle.message("notification.title"))
                .setSubtitle(CodeScreenshoterBundle.message("notification.image.saved"))
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                notification.addAction(object : NotificationAction(CodeScreenshoterBundle.message("action.open.name")) {
                    override fun actionPerformed(anActionEvent: AnActionEvent, notification: Notification) {
                        try {
                            Desktop.getDesktop().open(path.toFile())
                        } catch (e: IOException) {
                            showError(
                                project, CodeScreenshoterBundle.message(
                                    "error.open.image",
                                    StringUtil.escapeXmlEntities(path.toString()) + ":<br>" +
                                            StringUtil.escapeXmlEntities(StringUtil.notNullize(e.localizedMessage))
                                )
                            )
                        }
                    }
                })
            }
            notification.notify(project)
        } catch (e: FileAlreadyExistsException) {
            showError(
                project, CodeScreenshoterBundle.message(
                    "error.save.image",
                    StringUtil.escapeXmlEntities(path.toString()) + ":<br>" +
                            CodeScreenshoterBundle.message("error.not.directory", path.toString()) + " " +
                            StringUtil.escapeXmlEntities(e.file)
                )
            )
        } catch (e: IOException) {
            showError(
                project, CodeScreenshoterBundle.message(
                    "error.save.image",
                    StringUtil.escapeXmlEntities(path.toString()) + ":<br>" +
                            StringUtil.escapeXmlEntities(StringUtil.notNullize(e.localizedMessage))
                )
            )
        }
    }

    /**
     * Updates the action's availability based on the current editor state.
     * The action is enabled only when there is a valid editor available.
     *
     * @param event The action event containing the editor context
     */
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getEditor(event) != null
    }

    /**
     * Specifies the thread on which the action updates should be performed.
     * This ensures UI updates happen on the Event Dispatch Thread (EDT).
     *
     * @return ActionUpdateThread.EDT to run updates on the EDT
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    /**
     * Returns the message to show when the action is unavailable (e.g., not in a text editor).
     * This method must be implemented by subclasses to provide context-specific messages.
     *
     * @return String containing the unavailable message
     */
    protected abstract fun getUnavailableMessage(): String

    /**
     * Returns the message to show when no text is selected in the editor.
     * This method must be implemented by subclasses to provide context-specific messages.
     *
     * @return String containing the no selection message
     */
    protected abstract fun getNoSelectionMessage(): String

    /**
     * Returns the warning message shown when attempting to create a large image.
     * This method must be implemented by subclasses to provide context-specific messages.
     *
     * @param currentSettle The current settle value
     * @return String containing the large image warning message
     */
    protected abstract fun getWarningMessage(currentSettle: String): String

    /**
     * Returns the text to display on the "Yes" button in confirmation dialogs.
     * This method must be implemented by subclasses to provide context-specific messages.
     *
     * @return String containing the yes button text
     */
    protected abstract fun getYesButtonText(): String
}
