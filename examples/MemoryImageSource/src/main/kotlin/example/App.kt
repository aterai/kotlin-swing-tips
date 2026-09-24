package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import java.awt.image.MemoryImageSource
import javax.swing.*
import kotlin.math.abs
import kotlin.math.roundToInt

fun createUI(): JComponent = PaintPanel().also {
  it.preferredSize = Dimension(320, 240)
}

private class PaintPanel :
  JPanel(),
  MouseMotionListener,
  MouseListener {
  private val startPoint = Point()
  private val imageRect = Rectangle(320, 240)
  private val backImage = BufferedImage(
    imageRect.width,
    imageRect.height,
    BufferedImage.TYPE_INT_ARGB,
  )
  private val pixels = IntArray(imageRect.width * imageRect.height)
  private val source = MemoryImageSource(
    imageRect.width,
    imageRect.height,
    pixels,
    0,
    imageRect.width,
  )
  private val image: Image
  private var penColor = 0

  init {
    addMouseMotionListener(this)
    addMouseListener(this)
    // Reuse a single Image and send only the changed area with newPixels(...)
    source.setAnimated(true)
    image = Toolkit.getDefaultToolkit().createImage(source)
    val g2 = backImage.createGraphics()
    g2.paint = TEXTURE
    g2.fill(imageRect)
    g2.dispose()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.drawImage(backImage, 0, 0, this)
    g2.drawImage(image, 0, 0, this)
    g2.dispose()
  }

  // Draws a line of 3 x 3 stamps from p0 to p1 into the pixel array
  private fun drawLine(
    p0: Point,
    p1: Point,
  ) {
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    val steps = maxOf(abs(dx), abs(dy))
    var dirty: Rectangle? = null
    for (i in 0..steps) {
      val px = if (steps == 0) p0.x else p0.x + (dx * i / steps.toFloat()).roundToInt()
      val py = if (steps == 0) p0.y else p0.y + (dy * i / steps.toFloat()).roundToInt()
      val r = paintStamp(px, py)
      if (!r.isEmpty) {
        dirty = dirty?.union(r) ?: r
      }
    }
    dirty?.also {
      source.newPixels(it.x, it.y, it.width, it.height)
      repaint(it)
    }
  }

  // Fills a 3 x 3 square centered on (px, py), clipped to the image bounds
  private fun paintStamp(
    px: Int,
    py: Int,
  ): Rectangle {
    val r = Rectangle(px - 1, py - 1, 3, 3).intersection(imageRect)
    for (y in r.y until r.y + r.height) {
      val offset = y * imageRect.width
      pixels.fill(penColor, offset + r.x, offset + r.x + r.width)
    }
    return r
  }

  override fun mousePressed(e: MouseEvent) {
    startPoint.location = e.point
    penColor = if (SwingUtilities.isLeftMouseButton(e)) PEN_COLOR else ERASER_COLOR
    drawLine(startPoint, startPoint)
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    drawLine(startPoint, pt)
    startPoint.location = pt
  }

  override fun mouseMoved(e: MouseEvent) {
    // not needed
  }

  override fun mouseExited(e: MouseEvent) {
    // not needed
  }

  override fun mouseEntered(e: MouseEvent) {
    // not needed
  }

  override fun mouseReleased(e: MouseEvent) {
    // not needed
  }

  override fun mouseClicked(e: MouseEvent) {
    // not needed
  }

  companion object {
    private val TEXTURE = createCheckerTexture(6, Color(0x32_C8_96_64, true))
    private const val PEN_COLOR = 0xFF_00_00_00.toInt()
    private const val ERASER_COLOR = 0x0

    fun createCheckerTexture(
      cellSize: Int,
      color: Color?,
    ): TexturePaint {
      val size = cellSize * cellSize
      val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.paint = color
      g2.fillRect(0, 0, size, size)
      var i = 0
      while (i * cellSize < size) {
        var j = 0
        while (j * cellSize < size) {
          if ((i + j) % 2 == 0) {
            g2.fillRect(i * cellSize, j * cellSize, cellSize, cellSize)
          }
          j++
        }
        i++
      }
      g2.dispose()
      return TexturePaint(img, Rectangle(size, size))
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
