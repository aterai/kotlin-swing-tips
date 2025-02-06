package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("SplitPaneDivider.draggingColor", Color(0x64_FF_64_64, true))
  return JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
    it.topComponent = JScrollPane(JTable(5, 3))
    it.bottomComponent = JScrollPane(JTree())
    it.dividerSize = 24
    // it.isContinuousLayout = false
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
