package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

/**
 * JLayeredPane1.
 *
 * @author Taka
 */
private const val BACK_LAYER = 1
private val FONT = Font(Font.MONOSPACED, Font.PLAIN, 12)
private val COLORS = intArrayOf(
  0xDD_DD_DD, 0xAA_AA_FF, 0xFF_AA_AA, 0xAA_FF_AA, 0xFF_FF_AA, 0xFF_AA_FF, 0xAA_FF_FF
)

fun makeUI(): Component {
  val cl: ClassLoader = Thread.currentThread().contextClassLoader
  val layerPane = BackImageLayeredPane(ImageIcon(cl.getResource("example/tokeidai.jpg")).image)
  for (i in 0..6) {
    val p = createPanel(layerPane, i)
    p.setLocation(i * 70 + 20, i * 50 + 15)
    layerPane.add(p, BACK_LAYER)
  }
  return JPanel(BorderLayout()).also {
    it.add(layerPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getColor(i: Int, f: Float): Color {
  val r = ((i shr 16 and 0xFF) * f).toInt()
  val g = ((i shr 8 and 0xFF) * f).toInt()
  val b = ((i and 0xFF) * f).toInt()
  return Color(r, g, b)
}

private fun createPanel(layerPane: JLayeredPane, i: Int): JPanel {
  val s = "<html><font color=#333333>Header:$i</font></html>"
  val label = JLabel(s).also {
    it.font = FONT
    it.isOpaque = true
    it.horizontalAlignment = SwingConstants.CENTER
    it.background = getColor(COLORS[i], .85f)
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
    it.background = Color(COLORS[i])
    it.border = BorderFactory.createLineBorder(getColor(COLORS[i], .5f), 1)
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
    (e.component as? JComponent)?.also {
      val dx = e.x - origin.x
      val dy = e.y - origin.y
      val pt = it.location
      it.setLocation(pt.x + dx, pt.y + dy)
    }
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
