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

internal class AdvancedImageBuilder(private val options: CopyImageOptionsProvider.State) {
    private var snapshot: StateSnapshot? = null

    fun isCapable(editor: Editor?): Boolean {
        return editor is EditorEx
    }

    fun takeScreenshot(_e: Editor?): BufferedImage {
        val editor = _e as EditorEx

        snapshot = StateSnapshot(editor)
        snapshot!!.tweakEditor(options)

        val bounds = calculateSelectionArea(editor, snapshot!!.selectionStart, snapshot!!.selectionEnd)

        val scaleRatio: Double = options.scale
        val padding: Int = options.padding

        val gutterCompWidth = editor.gutterComponentEx.getWidth()
        val innerFrameWidth = ((gutterCompWidth + bounds.width) * scaleRatio).toInt()
        val innerFrameHeight = (bounds.height * scaleRatio).toInt()
        val frameWidth = (innerFrameWidth + 2 * padding * scaleRatio).toInt()
        val frameHeight = (innerFrameHeight + 2 * padding * scaleRatio).toInt()
        val img = UIUtil.createImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB)

        val palette = img.createGraphics()
        palette.scale(scaleRatio, scaleRatio)
        val framePalette = palette.create(0, 0, frameWidth, frameHeight)
        val innerPalette = palette.create(padding, padding, innerFrameWidth, innerFrameHeight)

        padding(framePalette, editor, gutterCompWidth)
        content(innerPalette, editor, bounds)

        innerPalette.dispose()
        framePalette.dispose()
        palette.dispose()

        snapshot!!.restore()
        return img
    }

    /**
     * Draw background
     *
     * @param divider x position of gutter separator
     */
    private fun padding(palette: Graphics, editor: EditorEx, divider: Int) {
        val padding: Int = options.padding
        if (padding <= 0) return

        // FIXME: handle editor background image
        val paintHeight = palette.clipBounds.height
        drawGutterBackground(palette, editor, paintHeight, padding, divider)
        drawEditorBackground(palette, editor, paintHeight, padding, divider, palette.clipBounds.width)
    }

    /** Draw gutter and editor  */
    private fun content(palette: Graphics, editor: EditorEx, bounds: Rectangle) {
        val gutterComp: JComponent = editor.gutterComponentEx
        val contentComp = editor.contentComponent

        val gutterPalette = palette.create(
            0, -bounds.y,
            gutterComp.getWidth(), bounds.height + bounds.y
        )
        val contentPalette = palette.create(
            gutterComp.getWidth(), -bounds.y,
            bounds.width, bounds.height + bounds.y
        )
        contentPalette.translate(-bounds.x, 0)

        gutterComp.paint(gutterPalette)
        contentComp.paint(contentPalette)

        gutterPalette.dispose()
        contentPalette.dispose()
    }

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

    /** Store editor state before capture  */
    private class StateSnapshot(val editor: EditorEx) {
        val caretOffset: Int

        val selectionStart: Int
        val selectionEnd: Int

        // editor
        val isIndentGuidesShown: Boolean
        val isInnerWhitespaceShown: Boolean

        // gutter
        val isLineNumberShown: Boolean
        val isGutterIconShown: Boolean
        val isFoldingOutlineShown: Boolean

        init {
            val caretModel = editor.caretModel
            caretOffset = caretModel.offset

            val selectionModel = editor.selectionModel
            selectionStart = selectionModel.selectionStart
            selectionEnd = selectionModel.selectionEnd

            val settings = editor.settings
            isLineNumberShown = settings.isLineNumbersShown
            isGutterIconShown = settings.areGutterIconsShown()
            isFoldingOutlineShown = settings.isFoldingOutlineShown
            isIndentGuidesShown = settings.isIndentGuidesShown
            isInnerWhitespaceShown = settings.isInnerWhitespaceShown
        }

        /** Update editor appearances according to user provided setting.  */
        fun tweakEditor(options: CopyImageOptionsProvider.State) {
            editor.selectionModel.setSelection(0, 0) // clear selection

            if (options.removeCaret) {
                editor.setCaretEnabled(false)
                val start = editor.selectionModel.selectionStart
                val document = editor.document
                val caretModel = editor.caretModel
                caretModel.moveToOffset(if (start == 0) document.getLineEndOffset(document.lineCount - 1) else 0)
            }

            val settings = editor.settings
            settings.isLineNumbersShown = options.lineNumbersShown
            settings.setGutterIconsShown(options.gutterIconsShown)
            settings.isFoldingOutlineShown = options.foldingOutlineShown
            settings.isInnerWhitespaceShown = options.innerWhitespaceShown
            settings.isIndentGuidesShown = options.indentGuidesShown
            editor.scrollPane.validate()
        }

        fun restore() {
            editor.setCaretEnabled(true)

            val caretModel = editor.caretModel
            caretModel.moveToOffset(caretOffset)

            val selectionModel = editor.selectionModel
            selectionModel.setSelection(selectionStart, selectionEnd)

            val settings = editor.settings
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
         * @param height  image height
         * @param divider x position of gutter separator
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
         * @param height     image height
         * @param divider    x position where editor content begin(on right side)
         * @param imageWidth width of editor content
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

        /** helper methods to execute private methods  */
        private fun findMethod(obj: Any, methodName: String, vararg paramTypes: Class<*>?): Method {
            try {
                return obj.javaClass.getDeclaredMethod(methodName, *paramTypes)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            }
        }

        /** helper methods to execute private methods  */
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
