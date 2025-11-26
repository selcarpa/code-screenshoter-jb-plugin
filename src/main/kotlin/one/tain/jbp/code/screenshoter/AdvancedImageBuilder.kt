package one.tain.jbp.code.screenshoter

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.softwrap.SoftWrapDrawingType
import com.intellij.openapi.util.TextRange
import com.intellij.ui.paint.LinePainter2D
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced image builder that handles the creation of code screenshots with various formatting options.
 * This class manages advanced image rendering including proper scaling, padding, gutter display,
 * and editor state modifications during the screenshot creation process.
 *
 * @property options The configuration options for image generation
 */
internal class AdvancedImageBuilder(private val options: CopyImageOptionsProvider.State) {
    private var snapshot: StateSnapshot? = null

    /**
     * Checks if the editor is compatible with advanced image building functionality.
     * This method verifies that the editor is an instance of EditorEx which has
     * the necessary advanced capabilities for image generation.
     *
     * @param editor The editor to check for compatibility
     * @return True if the editor supports advanced image building, false otherwise
     */
    fun isCapable(editor: Editor?): Boolean {
        return editor is EditorEx
    }

    /**
     * Captures a screenshot of the editor content with advanced formatting options.
     * This method creates a properly scaled and padded image that includes both
     * the gutter and editor content areas. It temporarily modifies the editor state
     * to ensure the screenshot matches user preferences, then restores the original state.
     *
     * @param _e The editor instance to capture
     * @return A BufferedImage containing the screenshot with applied formatting
     */
    fun takeScreenshot(_e: Editor?): BufferedImage {
        val editor = _e as EditorEx

        // Create a snapshot of the current editor state to restore later
        snapshot = StateSnapshot(editor)
        // Apply user options to the editor for the screenshot
        snapshot!!.tweakEditor(options)

        // Calculate the area to capture based on the selection
        val bounds = calculateSelectionArea(editor, snapshot!!.selectionStart, snapshot!!.selectionEnd)

        // Get scale and padding settings from user options
        val scaleRatio: Double = options.scale
        val padding: Int = options.padding

        // Calculate dimensions for the image with proper scaling and padding
        val gutterCompWidth = editor.gutterComponentEx.getWidth()
        val innerFrameWidth = ((gutterCompWidth + bounds.width) * scaleRatio).toInt()
        val innerFrameHeight = (bounds.height * scaleRatio).toInt()
        val frameWidth = (innerFrameWidth + 2 * padding * scaleRatio).toInt()
        val frameHeight = (innerFrameHeight + 2 * padding * scaleRatio).toInt()

        // Create the final image with calculated dimensions
        val img = UIUtil.createImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB)

        // Set up graphics context with proper scaling
        val palette = img.createGraphics()
        palette.scale(scaleRatio, scaleRatio)

        // Create graphics contexts for frame and content areas
        val framePalette = palette.create(0, 0, frameWidth, frameHeight)
        val innerPalette = palette.create(padding, padding, innerFrameWidth, innerFrameHeight)

        // Draw padding background and content
        padding(framePalette, editor, gutterCompWidth)
        content(innerPalette, editor, bounds)

        // Properly dispose of graphics contexts to free resources
        innerPalette.dispose()
        framePalette.dispose()
        palette.dispose()

        // Restore the original editor state
        snapshot!!.restore()
        return img
    }

    /**
     * Draws the background padding around the main content area of the screenshot.
     * This method handles both the gutter area background and the editor content background,
     * ensuring proper separation and styling based on the editor's original appearance.
     *
     * @param palette The graphics context to draw the padding on
     * @param editor The editor instance whose appearance is being captured
     * @param divider The x-coordinate position where the gutter separator is located
     */
    private fun padding(palette: Graphics, editor: EditorEx, divider: Int) {
        val padding: Int = options.padding
        if (padding <= 0) return

        // FIXME: handle editor background image
        val paintHeight = palette.clipBounds.height
        // Draw the gutter background area
        drawGutterBackground(palette, editor, paintHeight, padding, divider)
        // Draw the main editor content background
        drawEditorBackground(palette, editor, paintHeight, padding, divider, palette.clipBounds.width)
    }

    /**
     * Renders the main content of the editor including both gutter and code areas.
     * This method handles painting both the gutter component (line numbers, breakpoints, etc.)
     * and the main editor content component (the actual code text) to the screenshot image.
     *
     * @param palette The graphics context to draw the content on
     * @param editor The editor instance containing the content to capture
     * @param bounds The rectangle defining the area of content to capture
     */
    private fun content(palette: Graphics, editor: EditorEx, bounds: Rectangle) {
        val gutterComp: JComponent = editor.gutterComponentEx
        val contentComp = editor.contentComponent

        // Create graphics context for the gutter area
        val gutterPalette = palette.create(
            0, -bounds.y,
            gutterComp.getWidth(), bounds.height + bounds.y
        )
        // Create graphics context for the editor content area
        val contentPalette = palette.create(
            gutterComp.getWidth(), -bounds.y,
            bounds.width, bounds.height + bounds.y
        )
        // Translate the content palette to properly align the text
        contentPalette.translate(-bounds.x, 0)

        // Paint both gutter and content components to their respective palettes
        gutterComp.paint(gutterPalette)
        contentComp.paint(contentPalette)

        // Properly dispose of graphics contexts to free resources
        gutterPalette.dispose()
        contentPalette.dispose()
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
    private fun calculateSelectionArea(editor: EditorEx, selectionStart: Int, selectionEnd: Int): Rectangle {
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
            for (i in 0..lineCount) {
                // add soft warp stops
                for (softWrap in wrapModel.getSoftWrapsForLine(i)) {
                    val point = editor.offsetToXY(softWrap.start - 1)
                    rightEdge = max(rightEdge, point.x + wsCharWidth + symbolWidth1)
                }
                // add line ends
                val lastColumn = LogicalPosition(i, Int.MAX_VALUE)
                val offset = editor.logicalPositionToOffset(lastColumn)
                val point = editor.offsetToXY(offset)
                rightEdge = max(rightEdge, point.x)
                bottomEdge = point.y
            }
        } else {
            var indentRegion = true

            if (options.chopIndentation) leftEdge = Int.MAX_VALUE

            for (i in selectionStart..selectionEnd) {
                val pointOnScreen = editor.offsetToVisualPosition(i)
                val upperLeft = editor.visualPositionToXY(pointOnScreen)

                if (i == selectionStart) {
                    upperEdge = upperLeft.y
                } else if (i == selectionEnd) {
                    bottomEdge = upperLeft.y
                } else {
                    rightEdge = max(rightEdge, upperLeft.x)

                    val current = selectedText.get(i - selectionStart)
                    if (current == '\n') {
                        indentRegion = true
                    } else if (indentRegion && !Character.isWhitespace(current)) {
                        indentRegion = false
                        if (options.chopIndentation) {
                            leftEdge = min(leftEdge, upperLeft.x)
                        }
                    }

                    if (!wrapModel.isSoftWrappingEnabled) continue
                    if (wrapModel.getSoftWrap(i) != null) { // wrap sign on visual line start
                        leftEdge = min(leftEdge, upperLeft.x - symbolWidth2)
                    }
                    if (wrapModel.getSoftWrap(i + 1) != null) { // wrap sign on visual line end
                        rightEdge = max(rightEdge, upperLeft.x + wsCharWidth + symbolWidth1)
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
     * Stores the complete state of the editor before capturing a screenshot.
     * This class captures the current selections, caret position, and visual settings
     * so they can be temporarily modified for the screenshot and then restored afterward.
     * This ensures that the editor returns to its original state after the screenshot is taken.
     *
     * @property editor The editor instance whose state is being captured
     */
    private class StateSnapshot(val editor: EditorEx) {
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
            editor.selectionModel.setSelection(0, 0) // clear selection to ensure clean screenshot

            if (options.removeCaret) {
                // Hide the caret by disabling it and moving it out of view
                editor.setCaretEnabled(false)
                val start = editor.selectionModel.selectionStart
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

    companion object {
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
        private fun drawGutterBackground(g: Graphics, editor: EditorEx, height: Int, padding: Int, divider: Int) {
            val comp = editor.gutterComponentEx

            // use anonymous code block to restrict variable scope,
            // save some private methods which only be used once.

            // gutter background
            run {
                val gtBg = comp.getBackground()
                g.color = gtBg
                g.fillRect(0, 0, padding + divider, height)
            }
            // editor background
            run {
                val edBg = editor.backgroundColor
                g.color = edBg
                val gutterSeparatorX = comp.whitespaceSeparatorOffset
                val foldingAreaWidth: Int = invokeMethod<Int?>(
                    AdvancedImageBuilder.findMethod(
                        comp,
                        "getFoldingAreaWidth"
                    ), comp
                )!!
                g.fillRect(padding + gutterSeparatorX, 0, foldingAreaWidth, height)
            }
            // folding line
            run {
                val shown: Boolean = invokeMethod(
                    AdvancedImageBuilder.findMethod(
                        comp,
                        "isFoldingOutlineShown"
                    ), comp
                )!!
                if (!shown) return
                val olBg: Color? = invokeMethod(
                    findMethod(
                        comp,
                        "getOutlineColor",
                        Boolean::class.javaPrimitiveType
                    ), comp, false
                )
                g.color = olBg
                val x = (comp.whitespaceSeparatorOffset + padding).toDouble()
                val w: Double = invokeMethod(
                    AdvancedImageBuilder.findMethod(
                        comp,
                        "getStrokeWidth"
                    ), comp
                )!!
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
        private fun drawEditorBackground(
            g: Graphics, editor: EditorEx, height: Int, padding: Int, divider: Int,
            imageWidth: Int
        ) {
            g.color = editor.backgroundColor
            g.fillRect(
                padding + divider, 0,
                (imageWidth - divider) + padding, height
            )
        }

        /**
         * Helper method to find a private method in a class using reflection.
         * This is used to access internal IntelliJ editor methods that are not part of the public API.
         *
         * @param obj The object whose class contains the method
         * @param methodName The name of the method to find
         * @param paramTypes The parameter types of the method
         * @return The Method object representing the found method
         */
        private fun findMethod(obj: Any, methodName: String, vararg paramTypes: Class<*>?): Method {
            try {
                return obj.javaClass.getDeclaredMethod(methodName, *paramTypes)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            }
        }

        /**
         * Helper method to invoke a private method using reflection.
         * This allows access to internal IntelliJ editor functionality that is not exposed through public APIs.
         *
         * @param method The method to invoke
         * @param instance The object instance on which to invoke the method
         * @param params The parameters to pass to the method
         * @return The return value of the method invocation
         */
        private fun <T> invokeMethod(method: Method, instance: Any?, vararg params: Any?): T? {
            try {
                method.isAccessible = true
                return method.invoke(instance, *params) as T?
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            }
        }
    }
}
