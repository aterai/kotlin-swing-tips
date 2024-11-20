package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  ToolTipManager.sharedInstance().dismissDelay = Int.MAX_VALUE
  val cl = Thread.currentThread().contextClassLoader
  val icon = cl
    .getResource("example/wi0124-48.png")
    ?.openStream()
    ?.use(ImageIO::read)
    ?.let { ImageIcon(it) }
    ?: MissingIcon()
  val tabbedPane = TabThumbnailTabbedPane()
  tabbedPane.addTab("wi0124-48.png", null, JLabel(icon), "wi0124-48")
  addImageTab(tabbedPane, "example/GIANT_TCR1_2013.jpg")
  addImageTab(tabbedPane, "example/CRW_3857_JFR.jpg")
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addImageTab(tabbedPane: JTabbedPane, path: String) {
  val cl = Thread.currentThread().contextClassLoader
  val icon = cl
    .getResource(path)
    ?.openStream()
    ?.use(ImageIO::read)
    ?.let { ImageIcon(it) }
    ?: MissingIcon()
  val scroll = JScrollPane(JLabel(icon))
  val f = File(path)
  tabbedPane.addTab(f.name, null, scroll, "tooltip")
}

private class TabThumbnailTabbedPane : JTabbedPane() {
  private var current = -1

  private fun getTabThumbnail(index: Int): Component {
    var c = getComponentAt(index)
    var icon: Icon? = null
    if (c is JScrollPane) {
      c = c.viewport.view
      val d = c.preferredSize
      val newW = (d.width * SCALE).toInt()
      val newH = (d.height * SCALE).toInt()
      val image = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
      val g2 = image.createGraphics()
      g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR,
      )
      g2.scale(SCALE, SCALE)
      c.print(g2)
      g2.dispose()
      icon = ImageIcon(image)
    } else if (c is JLabel) {
      icon = c.icon
    }
    return JLabel(icon)
  }

  override fun createToolTip(): JToolTip? {
    val index = current
    if (index < 0) {
      return null
    }
    val p = JPanel(BorderLayout())
    p.border = BorderFactory.createEmptyBorder()
    p.add(JLabel(getTitleAt(index)), BorderLayout.NORTH)
    p.add(getTabThumbnail(index))
    val tip = object : JToolTip() {
      override fun getPreferredSize(): Dimension {
        val i = insets
        val d = p.preferredSize
        return Dimension(d.width + i.left + i.right, d.height + i.top + i.bottom)
      }
    }
    tip.component = this
    LookAndFeel.installColorsAndFont(
      p,
      "ToolTip.background",
      "ToolTip.foreground",
      "ToolTip.font",
    )
    tip.layout = BorderLayout()
    tip.add(p)
    return tip
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val idx = indexAtLocation(e.x, e.y)
    return if (current == idx) {
      super.getToolTipText(e)
    } else {
      current = idx
      null
    }
  }

  companion object {
    private const val SCALE = .15
  }
}

private fun makeMissingImage(): Image {
  val missingIcon: Icon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.translate(x, y)
    g2.fillRect(0, 0, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(gap, gap, w - gap, h - gap)
    g2.drawLine(gap, h - gap, w - gap, gap)
    g2.dispose()
  }

  override fun getIconWidth() = 1_000

  override fun getIconHeight() = 1_000
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
