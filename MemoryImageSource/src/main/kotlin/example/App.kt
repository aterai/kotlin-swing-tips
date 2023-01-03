package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.MemoryImageSource
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.abs

fun makeUI(): Component {
  return PaintPanel().also {
    it.preferredSize = Dimension(320, 240)
  }
}

private class PaintPanel : JPanel(), MouseMotionListener, MouseListener {
  private var startPoint = Point()
  private val backImage: BufferedImage
  private val rect = Rectangle(320, 240)
  private val pixels = IntArray(rect.width * rect.height)
  private val src = MemoryImageSource(rect.width, rect.height, pixels, 0, rect.width)
  private var penColor = 0

  init {
    addMouseMotionListener(this)
    addMouseListener(this)
    backImage = BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB)
    val g2 = backImage.createGraphics()
    g2.paint = TEXTURE
    g2.fill(rect)
    g2.dispose()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.drawImage(backImage, 0, 0, this)
    g2.drawImage(createImage(src), 0, 0, this)
    g2.dispose()
  }

  override fun mouseDragged(e: MouseEvent) {
    val dx = e.x - startPoint.getX()
    val dy = e.y - startPoint.getY()
    val delta = abs(dx).coerceAtLeast(abs(dy))
    val xi = dx / delta
    val yi = dy / delta
    var xs = startPoint.x.toDouble()
    var ys = startPoint.y.toDouble()
    val pt = Point2D.Double()
    var i = 0
    while (i < delta) {
      pt.setLocation(xs, ys)
      if (!rect.contains(pt)) {
        break
      }
      paintStamp(pt, penColor)
      xs += xi
      ys += yi
      i++
    }
    startPoint = e.point
  }

  private fun paintStamp(p: Point2D, pen: Int) {
    val px = p.x.toInt()
    val py = p.y.toInt()
    for (n in -1..1) {
      for (m in -1..1) {
        val t = px + n + (py + m) * rect.width
        if (t >= 0 && t < rect.width * rect.height) {
          pixels[t] = pen
        }
      }
    }
    repaint(px - 2, py - 2, 4, 4)
  }

  override fun mousePressed(e: MouseEvent) {
    startPoint = e.point
    penColor = if (e.button == MouseEvent.BUTTON1) 0xFF_00_00_00.toInt() else 0x0
  }

  override fun mouseMoved(e: MouseEvent) {
    /* not needed */
  }

  override fun mouseExited(e: MouseEvent) {
    /* not needed */
  }

  override fun mouseEntered(e: MouseEvent) {
    /* not needed */
  }

  override fun mouseReleased(e: MouseEvent) {
    /* not needed */
  }

  override fun mouseClicked(e: MouseEvent) {
    /* not needed */
  }

  companion object {
    private val TEXTURE = createCheckerTexture(6, Color(0x32_C8_96_64, true))
    fun createCheckerTexture(cs: Int, color: Color?): TexturePaint {
      val size = cs * cs
      val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.paint = color
      g2.fillRect(0, 0, size, size)
      var i = 0
      while (i * cs < size) {
        var j = 0
        while (j * cs < size) {
          if ((i + j) % 2 == 0) {
            g2.fillRect(i * cs, j * cs, cs, cs)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
