package one.tain.jbp.code.screenshoter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Consumer
import com.intellij.util.SystemProperties
import one.tain.jbp.code.screenshoter.CopyImageUtils.getEditor
import one.tain.jbp.code.screenshoter.CopyImageUtils.notificationGroup
import one.tain.jbp.code.screenshoter.CopyImageUtils.showError
import java.awt.Desktop
import java.awt.Toolkit
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val DATE_TIME_PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
const val SIZE_LIMIT_TO_WARN: Long = 3000000L

class CopyImageAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val startTime = System.currentTimeMillis()
        val project = event.project ?: return
        val editor = getEditor(event) ?: run {
            showError(project, "'Copy as Image' is available in text editors only")
            return
        }

        if (!editor.selectionModel.hasSelection()) {
            showError(project, "Please select the text fragment to copy")
            return
        }

        val imageBuilder = ImageBuilder(editor)
        if (imageBuilder.selectedSize > SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(
                    project,
                    "Copying such a big image could be slow and may take a lot of memory. Proceed?",
                    "Code Screenshots", "Yes, Copy It!", "Cancel", null
                ) != Messages.YES
            ) {
                return
            }
        }

        val image = imageBuilder.createImage() ?: return

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

    private fun saveImage(image: TransferableImage<*>, project: Project) {
        val options = CopyImageOptionsProvider.getInstance(project).state
        val toSave = options.directoryToSave?.takeIf { it.isNotBlank() } ?: SystemProperties.getUserHome()
        val now = LocalDateTime.now()
        val date = DATE_TIME_PATTERN.format(now)
        val fileName = "Shot_$date.${image.format.ext}"
        val path = Paths.get(FileUtil.toSystemDependentName(toSave), fileName)

        try {
            Files.createDirectories(path.parent)

            Files.newOutputStream(path).use { image.write(it) }

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
                                project,
                                "Cannot open image: ${StringUtil.escapeXmlEntities(path.toString())}:<br>" +
                                        StringUtil.escapeXmlEntities(StringUtil.notNullize(e.localizedMessage))
                            )
                        }
                    }
                })
            }
            notification.notify(project)
        } catch (e: FileAlreadyExistsException) {
            showError(
                project,
                "Cannot save image: ${StringUtil.escapeXmlEntities(path.toString())}:<br>" +
                        "Not a directory: ${StringUtil.escapeXmlEntities(e.file)}"
            )
        } catch (e: IOException) {
            showError(
                project,
                "Cannot save image: ${StringUtil.escapeXmlEntities(path.toString())}:<br>" +
                        StringUtil.escapeXmlEntities(StringUtil.notNullize(e.localizedMessage))
            )
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getEditor(event) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT


}
