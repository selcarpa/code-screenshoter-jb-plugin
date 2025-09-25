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
 */
abstract class TransferableImage<T> internal constructor(
    val format: Format,
    val transferee: T
) : Transferable {

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (isDataFlavorSupported(flavor)) {
            return transferee as Any
        }
        throw UnsupportedFlavorException(flavor)
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor> {
        return format.flavors
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return format.flavors.contains(flavor)
    }

    @Throws(IOException::class)
    abstract fun write(to: OutputStream)
}

/**
 * Enum representing supported image formats for clipboard transfer.
 */
enum class Format(val ext: String, vararg val flavors: DataFlavor) {
    /**
     * PNG format: produces high-quality raster images.
     */
    PNG("png", DataFlavor.imageFlavor) {
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
     */
    SVG(
        "svg",
        DataFlavor(String::class.java, "image/svg+xml; charset=utf-8"),
        DataFlavor.stringFlavor
    ) {
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
 */
private class Svg(private val image: String) : TransferableImage<String>(Format.SVG, image) {
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        return when {
            flavor.isFlavorTextType -> transferee as Any
            flavor.isRepresentationClassReader -> StringReader(transferee)
            else -> super.getTransferData(flavor)
        }
    }

    @Throws(IOException::class)
    override fun write(to: OutputStream) {
        to.use { outputStream ->
            outputStream.write(image.toByteArray(StandardCharsets.UTF_8))
        }
    }
}

/**
 * PNG image implementation for clipboard transfer.
 */
private class Png(private val image: BufferedImage) : TransferableImage<BufferedImage>(Format.PNG, image) {
    @Throws(IOException::class)
    override fun write(to: OutputStream) {
        to.use { outputStream ->
            ImageIO.write(image, "png", outputStream)
        }
    }
}
