package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val toolBar = JToolBar("ToolBar")
  toolBar.add(JCheckBox("JCheckBox"))
  toolBar.add(JTextField(10))
  val p = JPanel(BorderLayout())
  EventQueue.invokeLater {
    (p.topLevelAncestor as? Window)?.also { w ->
      (toolBar.ui as? BasicToolBarUI)?.also {
        val pt = w.location
        it.setFloatingLocation(pt.x + 120, pt.y + 160)
        it.setFloating(true, null)
      }
    }
  }
  p.add(toolBar, BorderLayout.NORTH)
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
