package one.tain.jbp.code.screenshoter

import com.intellij.ui.scale.JBUIScale.sysScale
import one.tain.jbp.code.screenshoter.TransferableImage.Png
import one.tain.jbp.code.screenshoter.TransferableImage.Svg
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
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JComponent

abstract class TransferableImage<T> internal constructor(val format: Format, val transferee: T) : Transferable {

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (isDataFlavorSupported(flavor)) {
            return transferee!!
        }
        throw UnsupportedFlavorException(flavor)
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor> {
        return format.flavors.clone()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return Arrays.stream(format.flavors).anyMatch { that: DataFlavor -> flavor.equals(that) }
    }

    @Throws(IOException::class)
    abstract fun write(to: OutputStream)

    internal class Png(image: BufferedImage) : TransferableImage<BufferedImage>(Format.PNG, image) {
        @Throws(IOException::class)
        override fun write(to: OutputStream) {
            ImageIO.write(transferee, "png", to)
        }
    }

    internal class Svg(image: String) : TransferableImage<String>(Format.SVG, image) {
        @Throws(UnsupportedFlavorException::class)
        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor.equals(DataFlavor.plainTextFlavor)) {
                return StringReader(transferee)
            } else {
                return super.getTransferData(flavor)
            }
        }

        @Throws(IOException::class)
        override fun write(to: OutputStream) {
            OutputStreamWriter(to, StandardCharsets.UTF_8).use { w ->
                w.write(transferee)
            }
        }
    }
}

enum class Format(val ext: String, vararg val flavors: DataFlavor) {
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
            val img = BufferedImage(
                (width * scale + 2 * padding).toInt(),
                (height * scale + 2 * padding).toInt(),
                BufferedImage.TYPE_INT_RGB
            )
            val g = img.graphics as Graphics2D
            Format.paint(g, contentComponent, at, width, height, backgroundColor, padding)
            return Png(img)
        }
    },
    SVG("svg",   DataFlavor(String::class.java, "image/svg+xml; charset=utf-8"), DataFlavor.stringFlavor, DataFlavor.plainTextFlavor) {
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
            Format.paint(g, contentComponent, at, width, height, backgroundColor, padding)
            val writer = CharArrayWriter()
            g.stream(writer, true)
            return Svg(writer.toString())
        }
    };

    @Throws(IOException::class)
    abstract fun paint(
        contentComponent: JComponent, at: AffineTransform,
        width: Int, height: Int, backgroundColor: Color, padding: Int
    ): TransferableImage<*>

    companion object {
        fun paint(
            g: Graphics2D,
            contentComponent: JComponent, at: AffineTransform,
            width: Int, height: Int, backgroundColor: Color, padding: Int
        ) {
            val scale = sysScale(contentComponent).toDouble()
            val scaledWidth = (width * scale).toInt()
            val scaledHeight = (height * scale).toInt()
            val imgWidth = scaledWidth + 2 * padding
            val imgHeight = scaledHeight + 2 * padding
            g.color = backgroundColor
            g.fillRect(0, 0, imgWidth, imgHeight)
            g.translate(padding, padding)
            g.clipRect(0, 0, scaledWidth, scaledHeight)
            g.transform(at)
            contentComponent.paint(g)
        }
    }
}
