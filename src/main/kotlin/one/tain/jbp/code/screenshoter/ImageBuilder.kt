package one.tain.jbp.code.screenshoter

import com.intellij.codeInsight.hint.EditorFragmentComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import one.tain.jbp.code.screenshoter.CopyImageOptionsProvider.Companion.getInstance
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.io.IOException
import java.util.*


internal class ImageBuilder(private val editor: Editor) {

    // The project associated with the editor, used for accessing project-specific settings
    private val project: Project = Objects.requireNonNull<Project>(editor.project)

    /**
     * Creates an image from the selected text in the editor based on the given rectangle.
     *
     * This method captures the visual representation of the selected code, applies
     * the specified formatting options, and returns a transferable image that can be
     * used for clipboard operations or saving to file.
     *
     * @param r The rectangle representing the bounds of the selected text area
     * @return A TransferableImage representing the captured content, or null if creation fails
     */
    fun createImage(r: Rectangle2D): TransferableImage<*>? {
        val state: EditorUtils.EditorState = EditorUtils.EditorState.from(editor)
        try {
            // Save the current editor state and reset UI elements that shouldn't appear in the image
            EditorUtils.resetEditor(editor, project)

            val options = getInstance(project).state
            val scale = options.scale

            // Get the content component and its graphics for image rendering
            val contentComponent = editor.contentComponent
            val contentGraphics = contentComponent.getGraphics() as? Graphics2D ?: return null
            val currentTransform = contentGraphics.transform

            // Create a new transform with the desired scaling applied
            val newTransform = AffineTransform(currentTransform)
            newTransform.scale(scale, scale)

            val format: Format = options.format

            // To flush glyph cache - ensures all text rendering elements are properly initialized
            format.paint(contentComponent, newTransform, 1, 1, JBColor.BLACK, 0)

            // Translate the transform to account for the rectangle's position
            newTransform.translate(-r.x, -r.y)

            // Actually paint the formatted content to create the image
            return format.paint(
                contentComponent, newTransform,
                (r.width * scale).toInt(), (r.height * scale).toInt(),
                EditorFragmentComponent.getBackgroundColor(editor, false), options.padding
            )
        } catch (e: IOException) {
            Logger.getInstance(ImageBuilder::class.java).error(e)
            return null
        } finally {
            // Restore the original editor state to maintain UI consistency
            state.restore(editor)
        }
    }
}
