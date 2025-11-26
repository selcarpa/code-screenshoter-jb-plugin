package one.tain.jbp.code.screenshoter

import com.intellij.ui.scale.JBUIScale.sysScale
import com.intellij.util.ui.UIUtil
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.*
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import javax.swing.JComponent

/**
 * Abstract base class for transferable image data that can be placed on system clipboard.
 * Provides common functionality for different image formats.
 *
 * This class implements the Transferable interface which allows the image data
 * to be transferred via the system clipboard to other applications.
 *
 * @param T The type of the underlying image data (e.g., BufferedImage for PNG, String for SVG)
 */
abstract class TransferableImage<T> internal constructor(
    val format: Format,
    val transferee: T
) : Transferable {

    /**
     * Gets the transfer data in the requested data flavor.
     * This method is called by the system when another application requests the clipboard data.
     *
     * @param flavor The requested data flavor for the transfer
     * @return The image data in the requested format, or throws UnsupportedFlavorException if not supported
     * @throws UnsupportedFlavorException If the requested flavor is not supported
     */
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (isDataFlavorSupported(flavor)) {
            return transferee as Any
        }
        throw UnsupportedFlavorException(flavor)
    }

    /**
     * Returns all supported data flavors for this transferable image.
     * This method tells the system what formats this image can be provided in.
     *
     * @return An array of supported DataFlavor objects
     */
    override fun getTransferDataFlavors(): Array<out DataFlavor> {
        return format.flavors
    }

    /**
     * Checks if the specified data flavor is supported by this transferable image.
     * This method is used to determine if the image can be provided in the requested format.
     *
     * @param flavor The data flavor to check
     * @return True if the flavor is supported, false otherwise
     */
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return format.flavors.contains(flavor)
    }

    /**
     * Writes the image data to the specified output stream.
     * This abstract method must be implemented by subclasses to handle format-specific writing.
     *
     * @param to The output stream to write the image data to
     * @throws IOException If there's an error writing the image data
     */
    @Throws(IOException::class)
    abstract fun write(to: OutputStream)
}

/**
 * Enum representing supported image formats for clipboard transfer.
 * Each format provides a way to create image representations of code selections
 * with different file types and capabilities.
 */
enum class Format(val ext: String, vararg val flavors: DataFlavor) {
    /**
     * PNG format: produces high-quality raster images.
     * This format creates a bitmap image that preserves code formatting and syntax highlighting.
     * PNG images are widely supported and maintain quality for code screenshots.
     */
    PNG("png", DataFlavor.imageFlavor) {
        /**
         * Creates a PNG image from the given component.
         * This method renders the editor content to a buffered image with the specified
         * dimensions, background color, and padding.
         *
         * @param contentComponent The editor component to render
         * @param at The affine transformation to apply to the content
         * @param width The width of the image area to render
         * @param height The height of the image area to render
         * @param backgroundColor The background color for the image
         * @param padding The amount of padding to add around the content
         * @return A TransferableImage instance representing the PNG image
         */
        override fun paint(
            contentComponent: JComponent,
            at: AffineTransform,
            width: Int,
            height: Int,
            backgroundColor: Color,
            padding: Int
        ): TransferableImage<*> {
            val scale = sysScale(contentComponent).toDouble()
            val imgWidth = (width * scale + 2 * padding).toInt()
            val imgHeight = (height * scale + 2 * padding).toInt()
            val img = UIUtil.createImage(
                imgWidth,
                imgHeight,
                BufferedImage.TYPE_INT_RGB
            )
            val g = img.graphics as Graphics2D
            paintComponent(g, contentComponent, at, width, height, backgroundColor, padding)
            g.dispose() // Properly dispose of graphics resource
            return Png(img)
        }
    },

    /**
     * SVG format: produces scalable vector graphics.
     * This format creates a vector representation of the code that can be scaled
     * without quality loss, making it ideal for documents and presentations.
     */
    SVG(
        "svg",
        DataFlavor(String::class.java, "image/svg+xml; charset=utf-8"),
        DataFlavor.stringFlavor
    ) {
        /**
         * Creates an SVG image from the given component.
         * This method renders the editor content to an SVG document with the specified
         * dimensions, background color, and padding.
         *
         * @param contentComponent The editor component to render
         * @param at The affine transformation to apply to the content
         * @param width The width of the image area to render
         * @param height The height of the image area to render
         * @param backgroundColor The background color for the image
         * @param padding The amount of padding to add around the content
         * @return A TransferableImage instance representing the SVG image
         * @throws IOException If there's an error creating the SVG image
         */
        @Throws(IOException::class)
        override fun paint(
            contentComponent: JComponent,
            at: AffineTransform,
            width: Int,
            height: Int,
            backgroundColor: Color,
            padding: Int
        ): TransferableImage<*> {
            val domImpl = GenericDOMImplementation.getDOMImplementation()
            val doc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null)
            val g = SVGGraphics2D(doc)
            val scale = sysScale(contentComponent).toDouble()
            val size = Dimension((width * scale + 2 * padding).toInt(), (height * scale + 2 * padding).toInt())
            g.setSVGCanvasSize(size)
            paintComponent(g, contentComponent, at, width, height, backgroundColor, padding)
            val writer = CharArrayWriter()
            g.stream(writer, true)
            g.dispose() // Properly dispose of graphics resource
            return Svg(writer.toString())
        }
    };

    /**
     * Creates an image of this format from the given component.
     * This abstract method must be implemented by each format to handle
     * the specific rendering and image creation logic.
     *
     * @param contentComponent The editor component to render
     * @param at The affine transformation to apply to the content
     * @param width The width of the image area to render
     * @param height The height of the image area to render
     * @param backgroundColor The background color for the image
     * @param padding The amount of padding to add around the content
     * @return A TransferableImage instance representing the specific format
     * @throws IOException If there's an error creating the image
     */
    @Throws(IOException::class)
    abstract fun paint(
        contentComponent: JComponent,
        at: AffineTransform,
        width: Int,
        height: Int,
        backgroundColor: Color,
        padding: Int
    ): TransferableImage<*>

    companion object {
        /**
         * Paints a component with the specified transformation and options.
         * This helper method handles the common rendering logic for different image formats,
         * applying scaling, padding, background color, and transformations.
         *
         * @param g The graphics context to draw to
         * @param contentComponent The editor component to render
         * @param at The affine transformation to apply to the content
         * @param width The width of the image area to render
         * @param height The height of the image area to render
         * @param backgroundColor The background color for the image
         * @param padding The amount of padding to add around the content
         */
        private fun paintComponent(
            g: Graphics2D,
            contentComponent: JComponent,
            at: AffineTransform,
            width: Int,
            height: Int,
            backgroundColor: Color,
            padding: Int
        ) {
            val scale = sysScale(contentComponent).toDouble()
            val scaledWidth = (width * scale).toInt()
            val scaledHeight = (height * scale).toInt()
            val imgWidth = scaledWidth + 2 * padding
            val imgHeight = scaledHeight + 2 * padding

            // Paint background
            g.color = backgroundColor
            g.fillRect(0, 0, imgWidth, imgHeight)

            // Apply padding and clip
            g.translate(padding, padding)
            g.clipRect(0, 0, scaledWidth, scaledHeight)

            // Apply transformation and paint component
            g.transform(at)
            contentComponent.paint(g)
        }
    }
}

/**
 * SVG image implementation for clipboard transfer.
 * This class wraps an SVG string representation of code and provides
 * the necessary functionality to transfer it via the system clipboard.
 */
private class Svg(private val image: String) : TransferableImage<String>(Format.SVG, image) {
    /**
     * Gets the transfer data in the requested data flavor for SVG format.
     * For SVG, the data can be provided in multiple formats:
     * - Text format for direct string access
     * - Reader format for streaming access
     * - Standard transferable format
     *
     * @param flavor The requested data flavor for the transfer
     * @return The SVG data in the requested format, or throws UnsupportedFlavorException if not supported
     * @throws UnsupportedFlavorException If the requested flavor is not supported
     */
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        return when {
            flavor.isFlavorTextType -> transferee as Any
            flavor.isRepresentationClassReader -> StringReader(transferee)
            else -> super.getTransferData(flavor)
        }
    }

    /**
     * Writes the SVG image data to the specified output stream.
     * This method writes the SVG string to the output stream using UTF-8 encoding.
     *
     * @param to The output stream to write the SVG image data to
     * @throws IOException If there's an error writing the SVG image data
     */
    @Throws(IOException::class)
    override fun write(to: OutputStream) {
        to.use { outputStream ->
            outputStream.write(image.toByteArray(StandardCharsets.UTF_8))
        }
    }
}

/**
 * PNG image implementation for clipboard transfer.
 * This class wraps a BufferedImage and provides the necessary functionality
 * to transfer it via the system clipboard.
 */
private class Png(private val image: BufferedImage) : TransferableImage<BufferedImage>(Format.PNG, image) {
    /**
     * Writes the PNG image data to the specified output stream.
     * This method uses the ImageIO class to write the BufferedImage in PNG format
     * to the output stream, which is the standard way to save PNG images.
     *
     * @param to The output stream to write the PNG image data to
     * @throws IOException If there's an error writing the PNG image data
     */
    @Throws(IOException::class)
    override fun write(to: OutputStream) {
        to.use { outputStream ->
            ImageIO.write(image, "png", outputStream)
        }
    }
}
