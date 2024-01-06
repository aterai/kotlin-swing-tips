package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.lang.invoke.MethodHandles
import java.time.LocalTime
import java.time.ZoneId
import java.util.logging.Logger
import javax.swing.*

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)

fun makeUI(): Component {
  val button = JButton("JButton JButton")
  val timer = Timer(4000) {
    printInfo(button, LocalTime.now(ZoneId.systemDefault()).toString())
  }

  val check1 = JCheckBox("setVisible", true)
  check1.addActionListener {
    button.isVisible = (it.source as? JCheckBox)?.isSelected == true
  }

  val check2 = JCheckBox("setEnabled", true)
  check2.addActionListener {
    button.isEnabled = (it.source as? JCheckBox)?.isSelected == true
  }

  val check3 = JCheckBox("start", true)
  check3.addActionListener {
    if ((it.source as? JCheckBox)?.isSelected == true) {
      timer.start()
    } else {
      timer.stop()
    }
  }

  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  button.addHierarchyListener { e ->
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
      printInfo(button, "SHOWING_CHANGED")
    }
    if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) {
      printInfo(button, "DISPLAYABILITY_CHANGED")
    }
  }
  printInfo(button, "after: new JButton, before: add(button); frame.setVisible(true)")

  val panel = JPanel()
  panel.add(button)
  for (i in 0 until 15) {
    panel.add(JLabel("<html>JLabel<br>&nbsp;idx:$i"))
  }
  tabs.addTab("Main", JScrollPane(panel))
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JLabel", JLabel("Test"))

  val p1 = JPanel(FlowLayout(FlowLayout.LEFT))
  p1.add(JLabel("JButton:"))
  p1.add(check1)
  p1.add(check2)

  val p2 = JPanel(FlowLayout(FlowLayout.LEFT))
  p2.add(JLabel("Timer:"))
  p2.add(check3)

  val p = JPanel(GridLayout(2, 1))
  p.add(p1)
  p.add(p2)
  timer.start()

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

fun printInfo(c: Component, str: String) {
  logger.info {
    """
      ${c.javaClass.name}: $str
        isDisplayable: ${c.isDisplayable}
        isShowing: ${c.isShowing}
        isVisible: ${c.isVisible}
    """.trimIndent()
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
