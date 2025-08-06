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
import com.intellij.util.SystemProperties
import one.tain.jbp.code.screenshoter.CopyImageOptionsProvider.Companion.getInstance
import one.tain.jbp.code.screenshoter.CopyImageUtils.getEditor
import one.tain.jbp.code.screenshoter.CopyImageUtils.notificationGroup
import one.tain.jbp.code.screenshoter.CopyImageUtils.showError
import java.awt.Desktop
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

class SaveImageAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = getEditor(event)
        if (editor == null) {
            showError(project, "'Save as Image' is available in text editors only")
            return
        }
        if (!editor.selectionModel.hasSelection()) {
            showError(project, "Please select the text fragment to save")
            return
        }

        val imageBuilder = ImageBuilder(editor)
        val (size, rectangle) = imageBuilder.selectedSize()
        if (size > SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(
                    project,
                    "Saving such a big image could be slow and may take a lot of memory. Proceed?",
                    "Code Screenshots", "Yes, Save It!", "Cancel", null
                ) != Messages.YES
            ) {
                return
            }
        }
        val image = imageBuilder.createImage(rectangle)
        if (image != null) {
            saveImage(image, project)
        }
    }

    private fun saveImage(image: TransferableImage<*>, project: Project) {
        val options = getInstance(project).state
        var toSave = options.directoryToSave
        if (StringUtil.isEmpty(toSave)) {
            toSave = SystemProperties.getUserHome()
        }
        toSave = toSave!!.trim { it <= ' ' }
        val now = LocalDateTime.now()
        val date = DATE_TIME_PATTERN.format(now)
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
                .setTitle("Code screenshots")
                .setSubtitle("Image was saved:")
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                notification.addAction(object : NotificationAction("Open") {
                    override fun actionPerformed(anActionEvent: AnActionEvent, notification: Notification) {
                        try {
                            Desktop.getDesktop().open(path.toFile())
                        } catch (e: IOException) {
                            showError(
                                project, "Cannot open image:  " + StringUtil.escapeXmlEntities(
                                    path.toString()
                                ) + ":<br>" + StringUtil.escapeXmlEntities(
                                    StringUtil.notNullize(e.localizedMessage)
                                )
                            )
                        }
                    }
                })
            }
            notification.notify(project)
        } catch (e: FileAlreadyExistsException) {
            showError(
                project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()
                ) + ":<br>Not a directory: " + StringUtil.escapeXmlEntities(e.file)
            )
        } catch (e: IOException) {
            showError(
                project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()
                ) + ":<br>" + StringUtil.escapeXmlEntities(
                    StringUtil.notNullize(e.localizedMessage)
                )
            )
        }
    }

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        presentation.isEnabled = getEditor(event) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
