package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

private const val MIN_TAB_WIDTH = 100

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsTabbedPaneUI) {
        object : WindowsTabbedPaneUI() {
          override fun calculateTabWidth(tabPlacement: Int, tabIndex: Int, metrics: FontMetrics) =
            MIN_TAB_WIDTH.coerceAtLeast(super.calculateTabWidth(tabPlacement, tabIndex, metrics))
        }
      } else {
        object : BasicTabbedPaneUI() {
          override fun calculateTabWidth(tabPlacement: Int, tabIndex: Int, metrics: FontMetrics) =
            MIN_TAB_WIDTH.coerceAtLeast(super.calculateTabWidth(tabPlacement, tabIndex, metrics))
        }
      }
      setUI(tmp)
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
