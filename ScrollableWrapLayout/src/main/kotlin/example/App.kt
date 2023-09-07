package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(makePanel(JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))))
  it.add(makePanel(ScrollableWrapPanel(ScrollableWrapLayout(FlowLayout.LEFT, 10, 10))))
  it.preferredSize = Dimension(320, 240)
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

private class ScrollableWrapPanel(layout: LayoutManager) : JPanel(layout), Scrollable {
  override fun getPreferredScrollableViewportSize(): Dimension? {
    val o = SwingUtilities.getUnwrappedParent(this)
    return (o as? JViewport)?.size ?: super.getPreferredSize()
  }

  override fun getScrollableUnitIncrement(visible: Rectangle, orientation: Int, dir: Int) = 32

  override fun getScrollableBlockIncrement(visible: Rectangle, orientation: Int, dir: Int) = 32

  override fun getScrollableTracksViewportWidth() = true

  override fun getScrollableTracksViewportHeight() = false
}

private class ScrollableWrapLayout(
  align: Int,
  horGap: Int,
  verGap: Int
) : FlowLayout(align, horGap, verGap) {
  private val fixedHorGap = horGap

  private fun getPreferredHorizontalGap(target: Container): Int {
    val insets = target.insets
    var columns = 0
    var width = target.width
    if (target.parent is JViewport) {
      width = target.parent.bounds.width
    }
    width -= insets.left + insets.right + fixedHorGap * 2
    for (i in 0 until target.componentCount) {
      val m = target.getComponent(i)
      if (m.isVisible) {
        val d = m.preferredSize
        if (width - d.width - fixedHorGap < 0) {
          columns = i
          break
        }
        width -= d.width + fixedHorGap
      }
    }
    return fixedHorGap + if (columns == 0) 0 else width / columns
  }

  override fun layoutContainer(target: Container) {
    hgap = getPreferredHorizontalGap(target)
    super.layoutContainer(target)
  }

  override fun preferredLayoutSize(target: Container): Dimension {
    val dim = super.preferredLayoutSize(target)
    synchronized(target.treeLock) {
      if (target.parent is JViewport) {
        dim.width = target.parent.bounds.width
        for (m in target.components) {
          if (m.isVisible) {
            val d = m.preferredSize
            dim.height = dim.height.coerceAtLeast(d.height + m.y)
          }
        }
      }
      return dim
    }
  }
}

private data class ListItem(val title: String, val icon: Icon)

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
