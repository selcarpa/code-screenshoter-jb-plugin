package one.tain.jbp.code.screenshoter

import com.intellij.codeInsight.hint.EditorFragmentComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import one.tain.jbp.code.screenshoter.CopyImageOptionsProvider.Companion.getInstance
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.io.IOException
import java.util.*
import kotlin.math.min




internal class ImageBuilder(private val editor: Editor) {

    // Regular expression pattern to match empty suffixes (newlines followed by whitespace at the end)
    private val emptySuffix = java.util.regex.Pattern.compile("\n\\s+$")

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
        val state: EditorState = EditorState.from(editor)
        try {
            // Save the current editor state and reset UI elements that shouldn't appear in the image
            resetEditor()

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

    /**
     * Resets the editor's visual state before creating an image.
     *
     * This method removes visual elements like selections and carets that shouldn't
     * appear in the final image, according to the user's configuration options.
     * It temporarily disables the caret and selection highlighting to ensure
     * a clean image capture.
     */
    private fun resetEditor() {
        val options = getInstance(project).state

        // Remove the current text selection to avoid it appearing in the image
        editor.selectionModel.removeSelection()

        // If configured to remove caret from the image
        if (options.removeCaret) {
            val document = editor.document
            val range: TextRange = getRange(editor)

            // Move caret to a position that won't affect the visual representation
            val targetOffset = if (range.startOffset == 0) {
                // If selection starts at the beginning, move caret to end of document
                document.getLineEndOffset(document.lineCount - 1)
            } else {
                // Otherwise move caret to the beginning
                0
            }

            editor.caretModel.moveToOffset(targetOffset)

            // Disable the caret rendering if this is an extended editor
            if (editor is EditorEx) {
                editor.setCaretEnabled(false)
            }

            // Hide the caret row highlighting
            editor.settings.isCaretRowShown = false
        }
    }

    /**
     * Calculates the size of the selected area including padding and scaling.
     *
     * This method determines the total pixel dimensions of the image that would be
     * created from the current selection, taking into account padding and scale options.
     *
     * @return A pair containing the total pixel count (width * height * scale^2) and the selection rectangle
     */
    fun selectedSize():Rectangle2D {
        val rectangle = this.selectionRectangle
        // Calculate the total pixel count after applying scale
        return rectangle
    }

    /**
     * Gets the bounding rectangle for the current selection in the editor.
     *
     * This property calculates the visual bounds of the selected text, including
     * any padding specified in the options.
     */
    private val selectionRectangle: Rectangle2D
        get() {
            val options = getInstance(project).state
            val range: TextRange = getRange(editor)
            val document = editor.document
            val text = document.getText(range)
            // Calculate the selection rectangle based on the text range and content
            return getSelectionRectangle(range, text, options)
        }

    /**
     * Calculates the bounding rectangle for the selected text range in the editor.
     * This method iterates through each character in the range to determine the
     * visual boundaries, taking into account options like indentation chopping
     * and inline inlays.
     *
     * @param range   the text range for which to calculate the rectangle
     * @param text    the actual text content of the range
     * @param options the copy image options that affect rendering
     * @return a Rectangle2D representing the bounding box of the selection
     */
    private fun getSelectionRectangle(
        range: TextRange,
        text: String,
        options: CopyImageOptionsProvider.State
    ): Rectangle2D {
        val start = range.startOffset
        val end = range.endOffset
        val r: Rectangle2D = Rectangle2D.Double()

        // Iterate through each character in the selection range to build the bounding rectangle
        for (i in start..end) {
            // Skip empty suffixes at the end of lines if chop indentation is enabled
            if (options.chopIndentation &&
                emptySuffix.matcher(text.take(min(i - start + 1, text.length))).find()
            ) {
                continue
            }

            // Convert the text offset to a visual position in the editor
            val pos = editor.offsetToVisualPosition(i)
            // Then convert the visual position to actual X,Y coordinates
            val point: Point2D = editor.visualPositionToXY(pos)

            // Include both the top and bottom points of the character in the rectangle
            // to ensure the full character height is captured
            includePoint(r, point)
            includePoint(r, Point2D.Double(point.x, point.y + editor.lineHeight))
        }

        // Include inline inlays (like icons or custom components) in the bounding rectangle
        // as these elements are visually part of the content and should be included in the image
        for (inlay in editor.inlayModel.getInlineElementsInRange(start, end)) {
            val bounds = inlay.bounds
            if (bounds != null) {
                r.add(bounds)
            }
        }
        return r
    }

    /**
     * Represents the visual state of an editor, used to save and restore the editor's
     * appearance before and after creating an image.
     *
     * @property range The selected text range
     * @property offset The current caret position
     * @property caretRow Whether the caret row should be shown
     */
    class EditorState internal constructor(
        private val range: TextRange,
        private val offset: Int,
        private val caretRow: Boolean
    ) {
        /**
         * Restores the editor to the saved state, including selection, caret position,
         * and caret row visibility.
         *
         * @param editor The editor instance to restore
         */
        fun restore(editor: Editor) {
            // Restore the caret row visibility setting
            editor.settings.isCaretRowShown = caretRow
            val selectionModel = editor.selectionModel
            val caretModel = editor.caretModel
            val provider = getInstance(editor.project ?: return)

            // If the original configuration had removeCaret enabled, restore the caret
            if (provider.state.removeCaret) {
                if (editor is EditorEx) {
                    editor.setCaretEnabled(true)
                }
                // Move caret back to its original position
                caretModel.moveToOffset(offset)
            }
            // Restore the original text selection
            selectionModel.setSelection(range.startOffset, range.endOffset)
        }

        companion object {
            /**
             * Creates an EditorState instance from the current state of an editor.
             *
             * @param editor The editor to capture the state from
             * @return An EditorState instance with the current editor state
             */
            fun from(editor: Editor): EditorState {
                val range: TextRange = getRange(editor)
                val offset = editor.caretModel.offset

                return EditorState(range, offset, editor.settings.isCaretRowShown)
            }
        }
    }

    companion object {

        /**
         * Gets the currently selected text range in the editor.
         *
         * @param editor The editor to get the selection from
         * @return The TextRange representing the selected text, or an empty range if nothing is selected
         */
        private fun getRange(editor: Editor): TextRange {
            val selectionModel = editor.selectionModel
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            return TextRange(start, end)
        }

        /**
         * Includes a point in the given rectangle, expanding the rectangle if necessary.
         *
         * @param r The rectangle to expand
         * @param p The point to include in the rectangle
         */
        private fun includePoint(r: Rectangle2D, p: Point2D?) {
            if (p == null) return
            if (r.isEmpty) {
                // If the rectangle is empty, set it to a small rectangle containing the point
                r.setFrame(p, Dimension(1, 1))
            } else {
                // Otherwise, expand the rectangle to include the point
                r.add(p)
            }
        }
    }
}
