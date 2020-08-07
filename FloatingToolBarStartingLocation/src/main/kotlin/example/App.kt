package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val toolbar = JToolBar("ToolBar")
  toolbar.add(JCheckBox("JCheckBox"))
  toolbar.add(JTextField(10))
  val p = JPanel(BorderLayout())
  EventQueue.invokeLater {
    (p.topLevelAncestor as? Window)?.also { w ->
      (toolbar.ui as? BasicToolBarUI)?.also {
        val pt = w.location
        it.setFloatingLocation(pt.x + 120, pt.y + 160)
        it.setFloating(true, null)
      }
    }
  }
  p.add(toolbar, BorderLayout.NORTH)
  p.add(Box.createHorizontalStrut(0), BorderLayout.WEST)
  p.add(Box.createHorizontalStrut(0), BorderLayout.EAST)
  p.add(JScrollPane(JTree()))
  p.preferredSize = Dimension(320, 240)
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
