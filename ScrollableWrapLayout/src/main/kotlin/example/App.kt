package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    add(makePanel(JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))))
    add(makePanel(ScrollableWrapPanel(ScrollableWrapLayout(FlowLayout.LEFT, 10, 10))))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makePanel(box: JPanel): Component {
    listOf(
      ListItem("red", ColorIcon(Color.RED)),
      ListItem("green", ColorIcon(Color.GREEN)),
      ListItem("blue", ColorIcon(Color.BLUE)),
      ListItem("cyan", ColorIcon(Color.CYAN)),
      ListItem("darkGray", ColorIcon(Color.DARK_GRAY)),
      ListItem("gray", ColorIcon(Color.GRAY)),
      ListItem("lightGray", ColorIcon(Color.LIGHT_GRAY)),
      ListItem("magenta", ColorIcon(Color.MAGENTA)),
      ListItem("orange", ColorIcon(Color.ORANGE)),
      ListItem("pink", ColorIcon(Color.PINK)),
      ListItem("yellow", ColorIcon(Color.YELLOW)),
      ListItem("black", ColorIcon(Color.BLACK)),
      ListItem("white", ColorIcon(Color.WHITE))
    ).forEach {
      val button = JButton(it.icon)
      val label = JLabel(it.title, SwingConstants.CENTER)
      val p = JPanel(BorderLayout())
      p.add(button)
      p.add(label, BorderLayout.SOUTH)
      box.add(p)
    }
    return JScrollPane(box)
  }
}

class ScrollableWrapPanel(layout: LayoutManager) : JPanel(layout), Scrollable {
  override fun getPreferredScrollableViewportSize(): Dimension? =
    (SwingUtilities.getUnwrappedParent(this) as? JViewport)?.getSize() ?: super.getPreferredSize()

  override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 32

  override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 32

  override fun getScrollableTracksViewportWidth() = true

  override fun getScrollableTracksViewportHeight() = false
}

class ScrollableWrapLayout(align: Int, hgap: Int, vgap: Int) : FlowLayout(align, hgap, vgap) {
  private val fixedHgap = hgap
  private fun getPreferredHorizontalGap(target: Container): Int {
    val insets = target.getInsets()
    var columns = 0
    var width = target.getWidth()
    if (target.getParent() is JViewport) {
      width = target.getParent().getBounds().width
    }
    width -= insets.left + insets.right + fixedHgap * 2
    for (i in 0 until target.getComponentCount()) {
      val m = target.getComponent(i)
      if (m.isVisible()) {
        val d = m.getPreferredSize()
        if (width - d.width - fixedHgap < 0) {
          columns = i
          break
        }
        width -= d.width + fixedHgap
      }
    }
    return fixedHgap + width / columns
  }

  override fun layoutContainer(target: Container) {
    hgap = getPreferredHorizontalGap(target)
    super.layoutContainer(target)
  }

  override fun preferredLayoutSize(target: Container): Dimension {
    val dim = super.preferredLayoutSize(target)
    synchronized(target.treeLock) {
      if (target.getParent() is JViewport) {
        dim.width = target.getParent().getBounds().width
        for (m in target.getComponents()) {
          if (m.isVisible()) {
            val d = m.getPreferredSize()
            dim.height = dim.height.coerceAtLeast(d.height + m.y)
          }
        }
      }
      return dim
    }
  }
}

data class ListItem(val title: String, val icon: Icon)

class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
