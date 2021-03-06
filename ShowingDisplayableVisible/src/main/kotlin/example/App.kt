package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val button = JButton("JButton JButton")
  val timer = Timer(4000) {
    printInfo(button, LocalTime.now(ZoneId.systemDefault()).toString())
  }

  val check1 = JCheckBox("setVisible", true)
  check1.addActionListener { e -> button.isVisible = (e.source as? JCheckBox)?.isSelected == true }

  val check2 = JCheckBox("setEnabled", true)
  check2.addActionListener { e -> button.isEnabled = (e.source as? JCheckBox)?.isSelected == true }

  val check3 = JCheckBox("start", true)
  check3.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      timer.start()
    } else {
      timer.stop()
    }
  }

  val tab = JTabbedPane()
  tab.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
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
  tab.addTab("Main", JScrollPane(panel))
  tab.addTab("JTree", JScrollPane(JTree()))
  tab.addTab("JLabel", JLabel("Test"))

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
    it.add(tab)
    it.preferredSize = Dimension(320, 240)
  }
}

fun printInfo(c: Component, str: String) {
  println(c.javaClass.name + ": " + str)
  println("  isDisplayable:" + c.isDisplayable)
  println("  isShowing:" + c.isShowing)
  println("  isVisible:" + c.isVisible)
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
