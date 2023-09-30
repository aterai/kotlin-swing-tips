package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

/**
 * JLayeredPane1.
 *
 * @author Taka
 */
private const val BACK_LAYER = 1
private val FONT = Font(Font.MONOSPACED, Font.PLAIN, 12)
private val COLORS = intArrayOf(
  0xDD_DD_DD,
  0xAA_AA_FF,
  0xFF_AA_AA,
  0xAA_FF_AA,
  0xFF_FF_AA,
  0xFF_AA_FF,
  0xAA_FF_FF,
)

fun makeUI(): Component {
  val path = "example/GIANT_TCR1_2013.jpg"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val layerPane = BackImageLayeredPane(img)
  for ((i, c) in COLORS.withIndex()) {
    val p = createPanel(layerPane, i, c)
    p.setLocation(i * 70 + 20, i * 50 + 15)
    layerPane.add(p, BACK_LAYER)
  }
  return JPanel(BorderLayout()).also {
    it.add(layerPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getColor(
  i: Int,
  f: Float,
): Color {
  val r = ((i shr 16 and 0xFF) * f).toInt()
  val g = ((i shr 8 and 0xFF) * f).toInt()
  val b = ((i and 0xFF) * f).toInt()
  return Color(r, g, b)
}

private fun createPanel(
  layerPane: JLayeredPane,
  idx: Int,
  cc: Int,
): JPanel {
  val s = "<html><font color=#333333>Header:$idx</font></html>"
  val label = JLabel(s).also {
    it.font = FONT
    it.isOpaque = true
    it.horizontalAlignment = SwingConstants.CENTER
    it.background = getColor(cc, .85f)
    it.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  }

  val text = JTextArea().also {
    it.margin = Insets(4, 4, 4, 4)
    it.lineWrap = true
    it.isOpaque = false
  }

  return JPanel(BorderLayout()).also {
    val li = DragMouseListener(layerPane)
    it.addMouseListener(li)
    it.addMouseMotionListener(li)
    it.add(label, BorderLayout.NORTH)
    it.add(text)
    it.isOpaque = true
    it.background = Color(cc)
    it.border = BorderFactory.createLineBorder(getColor(cc, .5f))
    it.size = Dimension(120, 100)
  }
}

private class DragMouseListener(private val parent: JLayeredPane) : MouseAdapter() {
  private val origin = Point()

  override fun mousePressed(e: MouseEvent) {
    origin.location = e.point
    parent.moveToFront(e.component)
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val pt = c.location
    c.setLocation(pt.x + e.x - origin.x, pt.y + e.y - origin.y)
  }
}

private class BackImageLayeredPane(private val bgImage: Image?) : JLayeredPane() {
  override fun isOptimizedDrawingEnabled() = false

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (bgImage != null) {
      val iw = bgImage.getWidth(this)
      val ih = bgImage.getHeight(this)
      val d = size
      var h = 0
      while (h < d.getHeight()) {
        var w = 0
        while (w < d.getWidth()) {
          g.drawImage(bgImage, w, h, this)
          w += iw
        }
        h += ih
      }
    }
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.LIGHT_GRAY
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
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
