package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

private val tabs = JTabbedPane()
private val panel: Component = JLabel("Preferences")

fun makeUI(): Component {
  val check = JCheckBox("Details")
  check.isFocusPainted = false
  check.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      (e.component as? AbstractButton)?.doClick()
    }
  })
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      tabs.addTab("Preferences", panel)
      tabs.selectedComponent = panel
    } else {
      tabs.remove(panel)
    }
  }

  val b = TabbedPaneWithCompBorder(check, tabs)
  tabs.addMouseListener(b)
  tabs.border = b
  tabs.addTab("Quick Preferences", JLabel("JLabel"))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TabbedPaneWithCompBorder(
  private val checkBox: JCheckBox,
  private val tab: JTabbedPane
) : Border, MouseListener, SwingConstants {
  private val rubberStamp: Container = JPanel()
  private val rect = Rectangle()

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val size = checkBox.preferredSize
    var xx = tab.size.width - size.width
    val lastTab = tab.getBoundsAt(tab.tabCount - 1)
    val tabEnd = lastTab.x + lastTab.width
    if (xx < tabEnd) {
      xx = tabEnd
    }
    rect.setBounds(xx, -2, size.width, size.height)
    SwingUtilities.paintComponent(g, checkBox, rubberStamp, rect)
  }

  override fun getBorderInsets(c: Component) = Insets(0, 0, 0, 0)

  override fun isBorderOpaque() = true

  private fun dispatchEvent(e: MouseEvent) {
    if (!rect.contains(e.x, e.y)) {
      return
    }
    checkBox.bounds = rect
    checkBox.dispatchEvent(SwingUtilities.convertMouseEvent(tab, e, checkBox))
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
