package one.tain.jbp.code.screenshoter

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.image.BufferedImage

/**
 * Advanced image builder that handles the creation of code screenshots with various formatting options.
 * This class manages advanced image rendering including proper scaling, padding, gutter display,
 * and editor state modifications during the screenshot creation process.
 *
 * @property options The configuration options for image generation
 */
internal class AdvancedImageBuilder(private val options: CopyImageOptionsProvider.State) {
    private var snapshot: EditorUtils.StateSnapshot? = null

    /**
     * Checks if the editor is compatible with advanced image building functionality.
     * This method verifies that the editor is an instance of EditorEx which has
     * the necessary advanced capabilities for image generation.
     *
     * @param editor The editor to check for compatibility
     * @return True if the editor supports advanced image building, false otherwise
     */
    fun isCapable(editor: Editor?): Boolean {
        return EditorUtils.isEditorCapable(editor)
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
        snapshot = EditorUtils.StateSnapshot(editor)
        // Apply user options to the editor for the screenshot
        snapshot!!.tweakEditor(options)

        // Calculate the area to capture based on the selection
        val bounds = EditorUtils.calculateSelectionArea(editor, snapshot!!.selectionStart, snapshot!!.selectionEnd)

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
        EditorUtils.drawGutterBackground(palette, editor, paintHeight, padding, divider)
        // Draw the main editor content background
        EditorUtils.drawEditorBackground(palette, editor, paintHeight, padding, divider, palette.clipBounds.width)
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
        val components = EditorUtils.extractVisualComponents(editor)
        val gutterComp = components.gutterComponent
        val contentComp = components.contentComponent

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
}
