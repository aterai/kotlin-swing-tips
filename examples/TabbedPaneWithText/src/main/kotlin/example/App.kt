package example

import java.awt.*
import javax.swing.*

const val TEXT = "<--1234567890"

fun makeUI(): Component {
  val tabs = object : JTabbedPane() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val fm = g.fontMetrics
      val stringWidth = fm.stringWidth(TEXT) + 10
      val x = size.width - stringWidth
      val lastTab = getBoundsAt(tabCount - 1)
      val tabEnd = lastTab.x + lastTab.width
      val xx = x.coerceAtLeast(tabEnd) + 5
      g.drawString(TEXT, xx, 18)
    }
  }
  tabs.addTab("title1", JLabel("tab1"))
  tabs.addTab("title2", JLabel("tab2"))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
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
