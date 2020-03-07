package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel()
  SwingUtilities.invokeLater { p.rootPane.jMenuBar = createMenuBar() }
  p.add(JScrollPane(JTextArea()))
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun createMenuBar(): JMenuBar {
  val menuBar = JMenuBar()
  menuBar.layout = object : FlowLayout(LEFT, 2, 2) {
    override fun preferredLayoutSize(target: Container): Dimension {
      synchronized(target.treeLock) {
        var targetWidth = target.size.width
        targetWidth = if (targetWidth == 0) Int.MAX_VALUE else targetWidth
        val insets = target.insets
        val hgap = hgap
        val vgap = vgap
        val maxWidth = targetWidth - insets.left - insets.right
        var height = vgap
        var rowWidth = hgap
        var rowHeight = 0
        val nmembers = target.componentCount
        for (i in 0 until nmembers) {
          val m = target.getComponent(i)
          if (m.isVisible) {
            val d = m.preferredSize
            if (rowWidth + d.width > maxWidth) {
              height += rowHeight
              rowWidth = hgap
              rowHeight = 0
            }
            rowWidth += d.width + hgap
            rowHeight = rowHeight.coerceAtLeast(d.height + vgap)
          }
        }
        height += rowHeight + insets.top + insets.bottom
        return Dimension(targetWidth, height)
      }
    }
  }
  listOf("File", "Edit", "View", "Navigate", "Code", "Analyze", "Refactor", "Build", "Run", "Help")
    .map { createMenu(it) }
    .forEach { menuBar.add(it) }
  return menuBar
}

private fun createMenu(key: String): JMenu {
  val menu = JMenu(key)
  menu.add("dummy1")
  menu.add("dummy2")
  menu.add("dummy3")
  return menu
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
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
