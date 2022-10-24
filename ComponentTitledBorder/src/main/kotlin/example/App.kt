package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

fun makeUI(): Component {
  val tree = JTree()
  val c = JCheckBox("CheckBox", true)
  c.addActionListener { tree.isEnabled = c.isSelected }
  c.isFocusPainted = false

  val l1 = JScrollPane(tree)
  l1.border = ComponentTitledBorder(c, l1, BorderFactory.createEtchedBorder())

  val icon = JLabel(UIManager.getIcon("FileChooser.detailsViewIcon"))
  val l2 = JLabel("<html>ComponentTitledBorder<br>+ JLabel + Icon")
  l2.border = ComponentTitledBorder(icon, l2, BorderFactory.createEtchedBorder())

  val b = JButton("Button")
  b.isFocusPainted = false
  val l3 = JLabel("ComponentTitledBorder + JButton")
  l3.border = ComponentTitledBorder(b, l3, BorderFactory.createEtchedBorder())

  return JPanel(GridLayout(3, 1, 5, 5)).also {
    it.add(l1)
    it.add(l2)
    it.add(l3)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ComponentTitledBorder(
  private val comp: Component,
  container: Container,
  private val border: Border
) : MouseAdapter(), Border, SwingConstants {
  init {
    if (comp is JComponent) {
      comp.isOpaque = true
    }
    container.addMouseListener(this)
    container.addMouseMotionListener(this)
  }

  override fun isBorderOpaque() = true

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    if (c is Container) {
      val borderInsets = border.getBorderInsets(c)
      val insets = getBorderInsets(c)
      val temp = (insets.top - borderInsets.top) / 2
      border.paintBorder(c, g, x, y + temp, width, height - temp)
      val size = comp.preferredSize
      val rect = Rectangle(OFFSET, 0, size.width, size.height)
      SwingUtilities.paintComponent(g, comp, c, rect)
      comp.bounds = rect
    }
  }

  override fun getBorderInsets(c: Component): Insets {
    val size = comp.preferredSize
    val insets = border.getBorderInsets(c)
    insets.top = insets.top.coerceAtLeast(size.height)
    return insets
  }

  private fun dispatchEvent(e: MouseEvent) {
    val src = e.component
    comp.dispatchEvent(SwingUtilities.convertMouseEvent(src, e, comp))
    src.repaint()
  }

  override fun mouseClicked(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseEntered(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseExited(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mousePressed(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseReleased(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseMoved(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    dispatchEvent(e)
  }

  companion object {
    private const val OFFSET = 5
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
