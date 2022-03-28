package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val scroll = JScrollPane(JTable(24, 3))
  scroll.verticalScrollBar.addAdjustmentListener { e ->
    (e.adjustable as? JScrollBar)?.also { scrollBar ->
      val m = scrollBar.model
      scrollBar.getComponent(0)?.isEnabled = m.value != m.maximum - m.extent
      scrollBar.getComponent(1)?.isEnabled = m.value != m.minimum
    }
  }
  return JPanel(GridLayout(0, 2)).also {
    it.add(JScrollPane(JTable(24, 3)))
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
