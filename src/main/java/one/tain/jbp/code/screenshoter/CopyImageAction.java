package one.tain.jbp.code.screenshoter;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends AnAction {

    static final long SIZE_LIMIT_TO_WARN = 3_000_000L;
    static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        long startTime = System.currentTimeMillis();
        Project project = event.getProject();
        if (project == null) return;
        Editor editor = CopyImagePlugin.getEditor(event);
        if (editor == null) {
            CopyImagePlugin.showError(project, "'Copy as Image' is available in text editors only");
            return;
        }
        if (!editor.getSelectionModel().hasSelection()) {
            CopyImagePlugin.showError(project, "Please select the text fragment to copy");
            return;
        }

        ImageBuilder imageBuilder = new ImageBuilder(editor);
        if (imageBuilder.getSelectedSize() > SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(event.getProject(),
                "Copying such a big image could be slow and may take a lot of memory. Proceed?",
                "Code Screenshots", "Yes, Copy It!", "Cancel", null) != Messages.YES) {
                return;
            }
        }
        TransferableImage<?> image = imageBuilder.createImage();

        if (image != null) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(image, (clipboard1, contents) -> {
            });
            NotificationGroupManager.getInstance().getNotificationGroup("Code Screenshots")
                .createNotification("Image was copied to the clipboard", NotificationType.INFORMATION)
                .setTitle("Code screenshots")
                .addAction(DumbAwareAction.create("Save to File", anActionEvent -> saveImage(image, project)))
                .notify(editor.getProject());
            long endTime = System.currentTimeMillis();
            Logger.getInstance(CopyImageAction.class).warn("Copied image in " + (endTime - startTime) + " ms");
        }
    }

    private void saveImage(@NotNull TransferableImage<?> image, @NotNull Project project) {
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
        String toSave = options.myDirectoryToSave;
        if (StringUtil.isEmpty(toSave)) {
            toSave = SystemProperties.getUserHome();
        }
        toSave = toSave.trim();
        LocalDateTime now = LocalDateTime.now();
        String date = DATE_TIME_PATTERN.format(now);
        String fileName = "Shot_" + date + "." + image.format.ext;
        Path path = Paths.get(FileUtil.toSystemDependentName(toSave), fileName);
        try {
            Files.createDirectories(path.getParent());

            try (OutputStream os = Files.newOutputStream(path)) {
                image.write(os);
            }

            String pathRepresentation = StringUtil.escapeXmlEntities(StringUtil.shortenPathWithEllipsis(path.toString(), 50));
            Notification notification = CopyImagePlugin.getNotificationGroup()
                    .createNotification(pathRepresentation, NotificationType.INFORMATION)
                    .setTitle("Code screenshots")
                    .setSubtitle("Image was saved:");
            if (Desktop.isDesktopSupported()) {
                notification.addAction(new NotificationAction("Open") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent anActionEvent, @NotNull Notification notification) {
                        try {
                            Desktop.getDesktop().open(path.toFile());
                        } catch (IOException e) {
                            CopyImagePlugin.showError(project, "Cannot open image:  " + StringUtil.escapeXmlEntities(
                                    path.toString()) + ":<br>" + StringUtil.escapeXmlEntities(
                                    StringUtil.notNullize(e.getLocalizedMessage())));
                        }
                    }
                });
            }
            notification.notify(project);
        } catch (FileAlreadyExistsException e) {
            CopyImagePlugin.showError(project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()) + ":<br>Not a directory: " + StringUtil.escapeXmlEntities(e.getFile()));
        } catch (IOException e) {
            CopyImagePlugin.showError(project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()) + ":<br>" + StringUtil.escapeXmlEntities(
                    StringUtil.notNullize(e.getLocalizedMessage())));
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
