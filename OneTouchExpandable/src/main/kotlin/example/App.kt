package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.lang.Boolean.TYPE
import java.security.AccessController
import java.security.PrivilegedAction
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicSplitPaneUI

fun makeUI(): Component {
  val s1 = object : JScrollPane(JTable(6, 3)) {
    override fun getMinimumSize() = Dimension(0, 100)
  }
  val s2 = object : JScrollPane(JTree()) {
    override fun getMinimumSize() = Dimension(0, 100)
  }

  val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  splitPane.topComponent = s1
  splitPane.bottomComponent = s2
  splitPane.isOneTouchExpandable = true
  if (splitPane.ui is BasicSplitPaneUI) {
    val pa = PrivilegedAction<Void?> {
      runCatching {
        splitPane.dividerLocation = 0
        val m = BasicSplitPaneUI::class.java.getDeclaredMethod("setKeepHidden", TYPE)
        m.isAccessible = true
        m.invoke(splitPane.ui, true)
      }
      null
    }
    AccessController.doPrivileged(pa)
  }

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
