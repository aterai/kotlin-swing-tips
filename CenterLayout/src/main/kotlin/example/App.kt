package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p1 = JPanel(GridBagLayout())
  val b1 = BorderFactory.createCompoundBorder(
    BorderFactory.createTitledBorder("GridBagLayout"),
    BorderFactory.createMatteBorder(5, 10, 15, 20, Color.RED)
  )
  p1.border = b1
  p1.add(JButton("GridBagLayout"))

  val p2 = JPanel(CenterLayout())
  val b2 = BorderFactory.createCompoundBorder(
    BorderFactory.createTitledBorder("CenterLayout"),
    BorderFactory.createMatteBorder(5, 10, 15, 20, Color.GREEN)
  )
  p2.border = b2
  p2.add(JButton("CenterLayout"))

  return JPanel(GridLayout(0, 1, 0, 10)).also {
    it.add(p1)
    it.add(p2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CenterLayout : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component) {
    /* not needed */
  }

  override fun removeLayoutComponent(comp: Component) {
    /* not needed */
  }

  override fun preferredLayoutSize(container: Container) = container.getComponent(0)?.let {
    val size = it.preferredSize
    val insets = container.insets
    size.width += insets.left + insets.right
    size.height += insets.top + insets.bottom
    size
  } ?: Dimension()

  override fun minimumLayoutSize(c: Container) = preferredLayoutSize(c)

  override fun layoutContainer(container: Container) {
    val c = container.getComponent(0)
    c.size = c.preferredSize
    if (container is JComponent) {
      val size = c.size
      val r = SwingUtilities.calculateInnerArea(container, null)
      val x = r.x + (r.width - size.width) / 2
      val y = r.y + (r.height - size.height) / 2
      c.setBounds(x, y, size.width, size.height)
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
