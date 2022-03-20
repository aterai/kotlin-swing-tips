package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val scroll = JScrollPane(JTable(100, 3))
  scroll.verticalScrollBar.addAdjustmentListener { e ->
    (e.adjustable as? JScrollBar)?.also { scrollBar ->
      val m = scrollBar.model
      when (m.value) {
        m.maximum - m.extent -> scrollBar.getComponent(0)?.isEnabled = false
        m.minimum -> scrollBar.getComponent(1)?.isEnabled = false
        else -> scrollBar.components.forEach { it.isEnabled = true }
      }
    }
  }
  return JPanel(GridLayout(0, 2)).also {
    it.add(JScrollPane(JTable(100, 3)))
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
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
