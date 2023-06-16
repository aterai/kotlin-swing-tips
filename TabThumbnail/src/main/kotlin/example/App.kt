package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  ToolTipManager.sharedInstance().dismissDelay = Int.MAX_VALUE
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/wi0124-48.png"))
  val tabbedPane = TabThumbnailTabbedPane()
  tabbedPane.addTab("wi0124-48.png", null, JLabel(icon), "wi0124-48")
  addImageTab(tabbedPane, cl.getResource("example/GIANT_TCR1_2013.jpg"))
  addImageTab(tabbedPane, cl.getResource("example/CRW_3857_JFR.jpg"))

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addImageTab(tabbedPane: JTabbedPane, url: URL?) {
  requireNotNull(url) { "Resource not found" }
  val scroll = JScrollPane(JLabel(ImageIcon(url)))
  val f = File(url.file)
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
        RenderingHints.VALUE_INTERPOLATION_BILINEAR
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
      "ToolTip.font"
    )
    tip.layout = BorderLayout()
    tip.add(p)
    return tip
  }

  override fun getToolTipText(e: MouseEvent): String? {
    var str: String? = null
    val index = indexAtLocation(e.x, e.y)
    if (current == index) {
      str = super.getToolTipText(e)
    }
    current = index
    return str
  }

  companion object {
    private const val SCALE = .15
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
