package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private var counter = 0

fun makeUI(): Component {
  val tabs = object : JTabbedPane() {
    override fun addTab(title: String, content: Component) {
      super.addTab(title, content)
      val check = JCheckBox().also {
        it.isOpaque = false
        it.isFocusable = false
      }
      val p = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).also {
        it.isOpaque = false
        it.add(check, BorderLayout.WEST)
        it.add(JLabel(title))
      }
      setTabComponentAt(tabCount - 1, p)
    }
  }
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JLabel", JLabel("JLabel"))

  val button = JButton("Add")
  button.addActionListener { addTab(tabs) }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun addTab(tabs: JTabbedPane) {
  val c = if (counter % 2 == 0) JTree() else JLabel("Tab$counter")
  tabs.addTab("Title$counter", c)
  tabs.selectedIndex = tabs.tabCount - 1
  counter++
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
