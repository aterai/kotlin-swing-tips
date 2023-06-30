package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneUI

fun makeUI(): Component {
  UIManager.put("SplitPane.oneTouchButtonSize", 32)
  UIManager.put("SplitPane.oneTouchButtonOffset", 50)
  UIManager.put("SplitPaneDivider.border", BorderFactory.createLineBorder(Color.RED, 10))
  UIManager.put("SplitPaneDivider.draggingColor", Color(0x64_FF_64_64, true))

  val splitPane = object : JSplitPane(VERTICAL_SPLIT) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        (ui as? BasicSplitPaneUI)?.divider?.also {
          it.background = Color.ORANGE
          for (c in it.components) {
            (c as? JButton)?.background = Color.ORANGE
          }
        }
      }
    }
  }
  splitPane.topComponent = JScrollPane(JTable(8, 3))
  splitPane.bottomComponent = JScrollPane(JTree())
  splitPane.dividerSize = 32
  splitPane.isOneTouchExpandable = true

  return JPanel(BorderLayout()).also {
    it.add(splitPane)
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
