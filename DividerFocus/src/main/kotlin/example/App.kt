package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneUI

fun makeUI(): Component {
  val splitPane = object : JSplitPane() {
    private var handler: MouseListener? = null

    override fun updateUI() {
      (ui as? BasicSplitPaneUI)?.divider?.removeMouseListener(handler)
      super.updateUI()
      handler = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          super.mousePressed(e)
          requestFocusInWindow()
        }
      }
      (ui as? BasicSplitPaneUI)?.divider?.addMouseListener(handler)
    }
  }
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default", JSplitPane()))
    it.add(makeTitledPanel("Divider.addMouseListener", splitPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
