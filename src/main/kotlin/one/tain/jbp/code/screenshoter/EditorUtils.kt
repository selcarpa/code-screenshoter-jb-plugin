package one.tain.jbp.code.screenshoter

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.softwrap.SoftWrapDrawingType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.paint.LinePainter2D
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.regex.Pattern
import javax.swing.JComponent
import kotlin.math.min

/**
 * Utility object containing editor-related functionality for the code screenshoter plugin.
 */
object EditorUtils {

    /**
     * Retrieves the editor instance from the given action event context.
     * This method extracts the editor from the action's data context, which is typically
     * available when the action is triggered from within an editor component.
     *
     * @param event The action event containing the editor context
     * @return The Editor instance if available, or null if no editor is associated with the event
     */
    fun getEditor(event: AnActionEvent): Editor? {
        val dataContext = event.dataContext
        return CommonDataKeys.EDITOR.getData(dataContext)
    }

    /**
     * Checks if the editor is compatible with advanced image building functionality.
     * This method verifies that the editor is an instance of EditorEx which has
     * the necessary advanced capabilities for image generation.
     *
     * @param editor The editor to check for compatibility
     * @return True if the editor supports advanced image building, false otherwise
     */
    fun isEditorCapable(editor: Editor?): Boolean {
        return editor is EditorEx
    }

    /**
     * Calculates the precise rectangular area of the editor that needs to be captured
     * based on the current selection or visible content. This method handles both
     * selected text and the case where no text is selected (entire visible area).
     * It also accounts for soft wrapping, indentation, and other layout considerations.
     *
     * @param editor The editor instance to calculate the selection area for
     * @param selectionStart The starting offset of the text selection
     * @param selectionEnd The ending offset of the text selection
     * @return A Rectangle representing the area to capture in the screenshot
     */
    fun calculateSelectionArea(editor: EditorEx, selectionStart: Int, selectionEnd: Int): Rectangle {
        val selectedView: Rectangle2D = Rectangle2D.Double()

        val selectedText = editor.document.getText(
            TextRange(selectionStart, selectionEnd)
        )

        val wrapModel = editor.softWrapModel
        val symbolWidth1 = wrapModel.getMinDrawingWidthInPixels(SoftWrapDrawingType.BEFORE_SOFT_WRAP_LINE_FEED)
        val symbolWidth2 = wrapModel.getMinDrawingWidthInPixels(SoftWrapDrawingType.AFTER_SOFT_WRAP)
        val wsCharWidth = EditorUtil.charWidth(' ', Font.PLAIN, editor)

        var leftEdge = 0
        var upperEdge = 0
        var rightEdge = 0
        var bottomEdge = 0

        if (selectedText.isEmpty()) { // user select nothing
            val lineCount = editor.document.lineCount
            for (i in 0 until lineCount) {
                // add soft wrap stops
                for (softWrap in wrapModel.getSoftWrapsForLine(i)) {
                    val point = editor.offsetToXY(softWrap.start - 1)
                    rightEdge = maxOf(rightEdge, point.x + wsCharWidth + symbolWidth1)
                }
                // add line ends
                val lastColumn = LogicalPosition(i, Int.MAX_VALUE)
                val offset = editor.logicalPositionToOffset(lastColumn)
                val point = editor.offsetToXY(offset)
                rightEdge = maxOf(rightEdge, point.x)
                bottomEdge = point.y
            }
        } else {
            // Cache the options to avoid repeated access
            val project = editor.project ?: return Rectangle()
            val options = CopyImageOptionsProvider.getInstance(project).state

            var indentRegion = true
            if (options.chopIndentation) leftEdge = Int.MAX_VALUE

            // Process by lines instead of character-by-character for better performance
            val startLine = editor.offsetToLogicalPosition(selectionStart).line
            val endLine = editor.offsetToLogicalPosition(selectionEnd).line

            for (line in startLine..endLine) {
                val lineStartOffset = editor.logicalPositionToOffset(LogicalPosition(line, 0))
                val lineEndOffset = if (line < editor.document.lineCount - 1) {
                    editor.logicalPositionToOffset(LogicalPosition(line + 1, 0)) - 1 // Exclude line break
                } else {
                    editor.document.textLength
                }

                // Use the intersection of line range and selection range
                val actualStart = maxOf(lineStartOffset, selectionStart)
                val actualEnd = minOf(lineEndOffset, selectionEnd)

                if (actualStart < actualEnd) {
                    // Process the line segment
                    for (i in actualStart until actualEnd) {
                        val pointOnScreen = editor.offsetToVisualPosition(i)
                        val upperLeft = editor.visualPositionToXY(pointOnScreen)

                        if (i == selectionStart) {
                            upperEdge = upperLeft.y
                        } else if (i == selectionEnd) {
                            bottomEdge = upperLeft.y
                        } else {
                            rightEdge = maxOf(rightEdge, upperLeft.x)

                            // Only check character and indentation when needed
                            if (options.chopIndentation) {
                                val current = selectedText.get(i - selectionStart)
                                if (current == '\n') {
                                    indentRegion = true
                                } else if (indentRegion && !Character.isWhitespace(current)) {
                                    indentRegion = false
                                    leftEdge = minOf(leftEdge, upperLeft.x)
                                }
                            }

                            if (wrapModel.isSoftWrappingEnabled) {
                                if (wrapModel.getSoftWrap(i) != null) { // wrap sign on visual line start
                                    leftEdge = minOf(leftEdge, upperLeft.x - symbolWidth2)
                                }
                                if (wrapModel.getSoftWrap(i + 1) != null) { // wrap sign on visual line end
                                    rightEdge = maxOf(rightEdge, upperLeft.x + wsCharWidth + symbolWidth1)
                                }
                            }
                        }
                    }
                }
            }
        }
        bottomEdge += editor.lineHeight
        val contentSize = editor.contentSize
        // there is a small cut off on right edge of the last character,
        // so extend width by 1 scaled pixel.
        contentSize.width = rightEdge - leftEdge + JBUI.scale(1)
        contentSize.height = bottomEdge - upperEdge
        selectedView.setFrame(Point(leftEdge, upperEdge), contentSize)
        return selectedView.bounds // case to int
    }

    /**
     * Represents the components needed for rendering the editor content.
     */
    data class ContentComponents(
        val gutterComponent: JComponent,
        val contentComponent: JComponent
    )

    /**
     * Extracts the visual components from the editor needed for content rendering.
     *
     * @param editor The editor to extract components from
     * @return ContentComponents object containing the necessary visual components
     */
    fun extractVisualComponents(editor: EditorEx): ContentComponents {
        return ContentComponents(
            gutterComponent = editor.gutterComponentEx,
            contentComponent = editor.contentComponent
        )
    }

    /**
     * Draws the background for the gutter area of the editor including the folding outline.
     * This method handles rendering the background colors and the folding separator line
     * to match the original editor's appearance in the screenshot.
     *
     * @param g The graphics context to draw on
     * @param editor The editor whose gutter background is being rendered
     * @param height The height of the area to draw
     * @param padding The padding around the content
     * @param divider The x-coordinate position where the gutter separator is located
     */
    fun drawGutterBackground(g: Graphics, editor: EditorEx, height: Int, padding: Int, divider: Int) {
        val visualProps = extractGutterVisualProperties(editor)

        // use anonymous code block to restrict variable scope,
        // save some methods which only be used once.

        // gutter background
        run {
            g.color = visualProps.backgroundColor
            g.fillRect(0, 0, padding + divider, height)
        }
        // editor background
        run {
            g.color = visualProps.contentBackgroundColor
            g.fillRect(padding + visualProps.separatorX, 0, visualProps.foldingAreaWidth, height)
        }
        // folding line - only draw if folding outline is enabled in editor settings
        run {
            if (!visualProps.foldingOutlineShown) return
            g.color = visualProps.outlineColor
            val x = (visualProps.separatorX + padding).toDouble()
            val w = 1.0 // standard stroke width
            LinePainter2D.paint(g as Graphics2D, x, 0.0, x, height.toDouble(), LinePainter2D.StrokeType.CENTERED, w)
        }
    }

    /**
     * Draws the background for the main editor content area.
     * This method renders the main content background matching the original editor's color.
     *
     * @param g The graphics context to draw on
     * @param editor The editor whose content background is being rendered
     * @param height The height of the area to draw
     * @param padding The padding around the content
     * @param divider The x-coordinate position where the gutter separator is located
     * @param imageWidth The total width of the image area
     */
    fun drawEditorBackground(
        g: Graphics, editor: EditorEx, height: Int, padding: Int, divider: Int,
        imageWidth: Int
    ) {
        val visualProps = extractContentVisualProperties(editor)
        g.color = visualProps.backgroundColor
        g.fillRect(
            padding + divider, 0,
            (imageWidth - divider) + padding, height
        )
    }

    /**
     * Represents the visual properties of the editor gutter needed for drawing.
     */
    private data class GutterVisualProperties(
        val backgroundColor: Color,
        val separatorX: Int,
        val foldingAreaWidth: Int,
        val foldingOutlineShown: Boolean,
        val outlineColor: Color?,
        val contentBackgroundColor: Color
    )

    /**
     * Extracts the visual properties from the editor needed for drawing the gutter background.
     *
     * @param editor The editor to extract properties from
     * @return GutterVisualProperties object containing all necessary visual properties
     */
    private fun extractGutterVisualProperties(editor: EditorEx): GutterVisualProperties {
        val comp = editor.gutterComponentEx
        val settings = editor.settings

        return GutterVisualProperties(
            backgroundColor = comp.background,
            separatorX = comp.whitespaceSeparatorOffset,
            foldingAreaWidth = getFoldingAreaWidth(editor),
            foldingOutlineShown = settings.isFoldingOutlineShown,
            outlineColor = editor.colorsScheme.defaultBackground,
            contentBackgroundColor = editor.backgroundColor
        )
    }

    /**
     * Represents the visual properties of the editor content needed for drawing.
     */
    private data class ContentVisualProperties(
        val backgroundColor: Color
    )

    /**
     * Extracts the visual properties from the editor needed for drawing the content background.
     *
     * @param editor The editor to extract properties from
     * @return ContentVisualProperties object containing all necessary visual properties
     */
    private fun extractContentVisualProperties(editor: EditorEx): ContentVisualProperties {
        return ContentVisualProperties(
            backgroundColor = editor.backgroundColor
        )
    }

    /**
     * Gets the folding area width from the editor settings.
     * This is calculated based on available editor properties instead of using reflection.
     */
    private fun getFoldingAreaWidth(editor: EditorEx): Int {
        // Get folding area width based on editor settings and properties
        // Since we can't directly access the private folding area width,
        // we estimate it based on typical values or editor properties
        val settings = editor.settings
        return if (settings.isFoldingOutlineShown) 10 else 0  // Typical folding margin width
    }

    /**
     * Stores the complete state of the editor before capturing a screenshot.
     * This class captures the current selections, caret position, and visual settings
     * so they can be temporarily modified for the screenshot and then restored afterward.
     * This ensures that the editor returns to its original state after the screenshot is taken.
     *
     * @property editor The editor instance whose state is being captured
     */
    class StateSnapshot(val editor: EditorEx) {
        val caretOffset: Int

        val selectionStart: Int
        val selectionEnd: Int

        // editor-specific settings that can be toggled for screenshots
        val isIndentGuidesShown: Boolean
        val isInnerWhitespaceShown: Boolean

        // gutter-specific settings that can be toggled for screenshots
        val isLineNumberShown: Boolean
        val isGutterIconShown: Boolean
        val isFoldingOutlineShown: Boolean

        init {
            val caretModel = editor.caretModel
            // Capture the current caret position to restore later
            caretOffset = caretModel.offset

            val selectionModel = editor.selectionModel
            // Capture the current selection range to restore later
            selectionStart = selectionModel.selectionStart
            selectionEnd = selectionModel.selectionEnd

            val settings = editor.settings
            // Capture current visual settings to restore after screenshot
            isLineNumberShown = settings.isLineNumbersShown
            isGutterIconShown = settings.areGutterIconsShown()
            isFoldingOutlineShown = settings.isFoldingOutlineShown
            isIndentGuidesShown = settings.isIndentGuidesShown
            isInnerWhitespaceShown = settings.isInnerWhitespaceShown
        }

        /**
         * Temporarily modifies the editor appearance according to user-specified options.
         * This method applies the screenshot-specific settings while maintaining the
         * ability to restore the original state after the screenshot is captured.
         *
         * @param options The user's configuration settings for the screenshot
         */
        fun tweakEditor(options: CopyImageOptionsProvider.State) {
            val selectionModel = editor.selectionModel
            selectionModel.setSelection(0, 0) // clear selection to ensure clean screenshot

            if (options.removeCaret) {
                // Hide the caret by disabling it and moving it out of view
                editor.setCaretEnabled(false)
                val start = selectionModel.selectionStart
                val document = editor.document
                val caretModel = editor.caretModel
                // Move caret to either end of document or to beginning to hide it
                caretModel.moveToOffset(if (start == 0) document.getLineEndOffset(document.lineCount - 1) else 0)
            }

            val settings = editor.settings
            // Apply user preferences for visual elements
            settings.isLineNumbersShown = options.lineNumbersShown
            settings.setGutterIconsShown(options.gutterIconsShown)
            settings.isFoldingOutlineShown = options.foldingOutlineShown
            settings.isInnerWhitespaceShown = options.innerWhitespaceShown
            settings.isIndentGuidesShown = options.indentGuidesShown
            editor.scrollPane.validate()
        }

        /**
         * Restores the editor to its original state as captured in the constructor.
         * This method ensures the editor returns to exactly how it was before the screenshot,
         * including all visual settings and selections.
         */
        fun restore() {
            // Re-enable the caret after screenshot
            editor.setCaretEnabled(true)

            val caretModel = editor.caretModel
            // Restore the original caret position
            caretModel.moveToOffset(caretOffset)

            val selectionModel = editor.selectionModel
            // Restore the original text selection
            selectionModel.setSelection(selectionStart, selectionEnd)

            val settings = editor.settings
            // Restore all original visual settings
            settings.isLineNumbersShown = isLineNumberShown
            settings.setGutterIconsShown(isGutterIconShown)
            settings.isFoldingOutlineShown = isFoldingOutlineShown
            settings.isIndentGuidesShown = isIndentGuidesShown
            settings.isInnerWhitespaceShown = isInnerWhitespaceShown
            editor.scrollPane.validate()
        }
    }

    /**
     * Gets the currently selected text range in the editor.
     *
     * @param editor The editor to get the selection from
     * @return The TextRange representing the selected text, or an empty range if nothing is selected
     */
    fun getRange(editor: Editor): TextRange {
        val selectionModel = editor.selectionModel
        return TextRange(selectionModel.selectionStart, selectionModel.selectionEnd)
    }

    /**
     * Includes a point in the given rectangle, expanding the rectangle if necessary.
     *
     * @param r The rectangle to expand
     * @param p The point to include in the rectangle
     */
    fun includePoint(r: Rectangle2D, p: Point2D?) {
        if (p == null) return
        if (r.isEmpty) {
            // If the rectangle is empty, set it to a small rectangle containing the point
            r.setFrame(p.x, p.y, 1.0, 1.0)
        } else {
            // Otherwise, expand the rectangle to include the point
            r.add(p)
        }
    }


    // Regular expression pattern to match empty suffixes (newlines followed by whitespace at the end)
    // Using a more efficient compiled pattern with DOTALL flag for better performance
    val EMPTY_SUFFIX: Pattern = Pattern.compile("\\n\\s+$", Pattern.DOTALL)

    /**
     * Calculates the bounding rectangle for the selected text range in the editor.
     * This method iterates through each character in the range to determine the
     * visual boundaries, taking into account options like indentation chopping
     * and inline inlays.
     *
     * @param editor The editor instance to calculate the rectangle for
     * @param range the text range for which to calculate the rectangle
     * @param text the actual text content of the range
     * @param options the copy image options that affect rendering
     * @return a Rectangle2D representing the bounding box of the selection
     */
    fun getSelectionRectangle(
        editor: Editor,
        range: TextRange,
        text: String,
        options: CopyImageOptionsProvider.State
    ): Rectangle2D {
        val start = range.startOffset
        val end = range.endOffset
        val r: Rectangle2D = Rectangle2D.Double()

        if (text.isEmpty()) {
            // If there's no text, just return an empty rectangle
            return r
        }

        // Cache the line height to avoid repeated access
        val lineHeight = editor.lineHeight

        // Check if we need to process the empty suffix logic (only if chopIndentation is enabled)
        val needEmptySuffixCheck = options.chopIndentation

        // Process by lines instead of character-by-character for better performance
        val startLine = editor.offsetToLogicalPosition(start).line
        val endLine = editor.offsetToLogicalPosition(end).line

        for (line in startLine..endLine) {
            val lineStartOffset = editor.logicalPositionToOffset(LogicalPosition(line, 0))
            val lineEndOffset = if (line < editor.document.lineCount - 1) {
                editor.logicalPositionToOffset(LogicalPosition(line + 1, 0)) - 1 // Exclude line break
            } else {
                editor.document.textLength
            }

            // Use the intersection of line range and selection range
            val actualStart = maxOf(lineStartOffset, start)
            val actualEnd = minOf(lineEndOffset, end)

            if (actualStart < actualEnd) {
                for (i in actualStart until actualEnd) {
                    // Skip empty suffixes at the end of lines if chop indentation is enabled
                    if (needEmptySuffixCheck &&
                        EMPTY_SUFFIX.matcher(text.substring(0, min(i - start + 1, text.length))).find()
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
                    includePoint(r, Point2D.Double(point.x, point.y + lineHeight))
                }
            }
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
     * Resets the editor's visual state before creating an image.
     *
     * This method removes visual elements like selections and carets that shouldn't
     * appear in the final image, according to the user's configuration options.
     * It temporarily disables the caret and selection highlighting to ensure
     * a clean image capture.
     *
     * @param editor The editor to reset
     * @param project The project associated with the editor
     */
    fun resetEditor(editor: Editor, project: Project) {
        val options = CopyImageOptionsProvider.getInstance(project).state

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
            // Get the project early to avoid multiple null checks
            val project = editor.project ?: return

            // Restore the caret row visibility setting
            editor.settings.isCaretRowShown = caretRow
            val selectionModel = editor.selectionModel
            val caretModel = editor.caretModel
            val provider = CopyImageOptionsProvider.getInstance(project)

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
}
