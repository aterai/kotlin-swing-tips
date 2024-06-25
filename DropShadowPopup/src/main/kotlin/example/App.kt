package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val popup0 = JPopupMenu()
  initPopupMenu(popup0)
  val popup1 = DropShadowPopupMenu()
  initPopupMenu(popup1)

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) } ?: MissingIcon()
  val label = JLabel(icon)
  label.componentPopupMenu = popup1

  val check = JCheckBox("Paint Shadow", true)
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    label.componentPopupMenu = if (b) popup1 else popup0
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initPopupMenu(p: JPopupMenu) {
  listOf("Open", "Save", "Close").forEach { p.add("$it(test)") }
  p.addSeparator()
  p.add("Exit").addActionListener { e ->
    val m = e.source
    if (m is JMenuItem) {
      (SwingUtilities.getUnwrappedParent(m) as? JPopupMenu)?.invoker?.also {
        SwingUtilities.getWindowAncestor(it)?.dispose()
      }
    }
  }
}

private class DropShadowPopupMenu : JPopupMenu() {
  private val dim = Dimension()
  private var shadow: BufferedImage? = null

  override fun updateUI() {
    border = null
    super.updateUI()
    val inner = border
    val outer = BorderFactory.createEmptyBorder(0, 0, OFFSET, OFFSET)
    border = BorderFactory.createCompoundBorder(outer, inner)
  }

  override fun isOpaque() = false

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.drawImage(shadow, 0, 0, this)
    g2.paint = background // ??? 1.7.0_03
    g2.fillRect(0, 0, width - OFFSET, height - OFFSET)
    g2.dispose()
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    val d = preferredSize
    val w = d.width
    val h = d.height
    if (dim.width != w || dim.height != h) {
      dim.setSize(w, h)
      shadow = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).also {
        val g2 = it.createGraphics()
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f)
        g2.paint = Color.BLACK
        for (i in 0..<OFFSET) {
          g2.fillRoundRect(
            OFFSET,
            OFFSET,
            w - OFFSET - OFFSET + i,
            h - OFFSET - OFFSET + i,
            4,
            4,
          )
        }
        g2.dispose()
      }
    }
    EventQueue.invokeLater {
      val top = SwingUtilities.getWindowAncestor(this)
      if (top?.type == Window.Type.POPUP) {
        top.background = Color(0x0, true)
      }
    }
    super.show(c, x, y)
  }

  companion object {
    private const val OFFSET = 4
  }
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
    g2.color = Color.RED
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
