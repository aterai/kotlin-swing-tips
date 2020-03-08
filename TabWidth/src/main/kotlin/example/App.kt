package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTabbedPaneUI

private const val MIN_TAB_WIDTH = 100

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      if (getUI() is WindowsTabbedPaneUI) {
        setUI(object : WindowsTabbedPaneUI() {
          override fun calculateTabWidth(tabPlacement: Int, tabIndex: Int, metrics: FontMetrics) =
            MIN_TAB_WIDTH.coerceAtLeast(super.calculateTabWidth(tabPlacement, tabIndex, metrics))
        })
      } else {
        setUI(object : BasicTabbedPaneUI() {
          override fun calculateTabWidth(tabPlacement: Int, tabIndex: Int, metrics: FontMetrics) =
            MIN_TAB_WIDTH.coerceAtLeast(super.calculateTabWidth(tabPlacement, tabIndex, metrics))
        })
      }
    }
  }
  val p = JPanel(GridLayout(2, 1, 0, 10))
  listOf(JTabbedPane(), tabbedPane)
    .forEach {
      it.addTab("111111", JLabel("JLabel 1"))
      it.addTab("22222222222222", JLabel("JLabel 2"))
      it.addTab("3", JLabel("JLabel 3"))
      p.add(it)
    }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
