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
import java.util.regex.Pattern
import kotlin.math.min


internal class ImageBuilder(private val editor: Editor) {
    private val project: Project = Objects.requireNonNull<Project>(editor.project)

    fun createImage(r:Rectangle2D): TransferableImage<*>? {

        val state: EditorState = EditorState.from(editor)
        try {
            resetEditor()
            val options = getInstance(project).state
            val scale = options.scale
            val contentComponent = editor.contentComponent
            val contentGraphics = contentComponent.getGraphics() as Graphics2D
            val currentTransform = contentGraphics.transform
            val newTransform = AffineTransform(currentTransform)
            newTransform.scale(scale, scale)
            val format: TransferableImage.Format = options.format
            // To flush glyph cache
            format.paint(contentComponent, newTransform, 1, 1, JBColor.BLACK, 0)

            newTransform.translate(-r.x, -r.y)

            return format.paint(
                contentComponent, newTransform,
                (r.width * scale).toInt(), (r.height * scale).toInt(),
                EditorFragmentComponent.getBackgroundColor(editor, false), options.padding
            )
        } catch (e: IOException) {
            Logger.getInstance(ImageBuilder::class.java).error(e)
            return null
        } finally {
            state.restore(editor)
        }
    }

    private fun resetEditor() {
        val document = editor.document
        val range: TextRange = getRange(editor)
        editor.selectionModel.removeSelection()
        if (getInstance(project).state.removeCaret) {
            editor.caretModel
                .moveToOffset(if (range.startOffset == 0) document.getLineEndOffset(document.lineCount - 1) else 0)
            if (editor is EditorEx) {
                editor.setCaretEnabled(false)
            }
            editor.settings.isCaretRowShown = false
        }
    }

    fun  selectedSize(): Pair<Long,Rectangle2D>{
            val options =
                getInstance(project).state
            val rectangle = this.selectionRectangle
            val sizeX = rectangle.width + options.padding * 2
            val sizeY = rectangle.height + options.padding * 2
            return (sizeX * sizeY * options.scale * options.scale).toLong() to rectangle
        }

    private val selectionRectangle: Rectangle2D
        get() {
            val options =
                getInstance(project).state
            val range: TextRange =
                getRange(editor)
            val document = editor.document
            val text = document.getText(range)
            return getSelectionRectangle(range, text, options)
        }

    /**
     *
     *
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

        // Iterate through each character in the selection range
        for (i in start..end) {
            // Skip empty suffixes at the end of lines if chop indentation is enabled
            if (options.chopIndentation &&
                EMPTY_SUFFIX.matcher(text.take(min(i - start + 1, text.length))).find()
            ) {
                continue
            }
            // Convert offset to visual position and then to XY coordinates
            val pos = editor.offsetToVisualPosition(i)
            val point: Point2D = editor.visualPositionToXY(pos)

            // Include both the top and bottom points of the character in the rectangle
            includePoint(r, point)
            includePoint(r, Point2D.Double(point.x, point.y + editor.lineHeight))
        }

        // Include inline inlays (like icons or custom components) in the bounding rectangle
        for (inlay in editor.inlayModel.getInlineElementsInRange(start, end)) {
            val bounds = inlay.bounds
            if (bounds != null) {
                r.add(bounds)
            }
        }
        return r
    }

    class EditorState internal constructor(
        private val range: TextRange,
        private val offset: Int,
        private val caretRow: Boolean
    ) {
        fun restore(editor: Editor) {
            editor.settings.isCaretRowShown = caretRow
            val selectionModel = editor.selectionModel
            val caretModel = editor.caretModel
            val provider =
                getInstance(Objects.requireNonNull<Project>(editor.project))
            if (provider.state.removeCaret) {
                if (editor is EditorEx) {
                    editor.setCaretEnabled(false)
                }
                caretModel.moveToOffset(offset)
            }
            selectionModel.setSelection(range.startOffset, range.endOffset)
        }

        companion object {
            fun from(editor: Editor): EditorState {
                val range: TextRange = getRange(editor)
                val offset = editor.caretModel.offset

                return EditorState(range, offset, editor.settings.isCaretRowShown)
            }
        }
    }

    companion object {
        private val EMPTY_SUFFIX: Pattern = Pattern.compile("\n\\s+$")

        private fun getRange(editor: Editor): TextRange {
            val selectionModel = editor.selectionModel
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            return TextRange(start, end)
        }

        private fun includePoint(r: Rectangle2D, p: Point2D?) {
            if (r.isEmpty) {
                r.setFrame(p, Dimension(1, 1))
            } else {
                r.add(p)
            }
        }
    }
}
