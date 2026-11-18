package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.lang.invoke.MethodHandles
import java.time.LocalTime
import java.time.ZoneId
import java.util.logging.Logger
import javax.swing.*

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)

fun createUI(): Component {
  val button = JButton("JButton JButton")
  val timer = Timer(4000) {
    printInfo(button, LocalTime.now(ZoneId.systemDefault()).toString())
  }

  val visibleCheck = JCheckBox("setVisible", true)
  visibleCheck.addActionListener {
    button.isVisible = visibleCheck.isSelected
  }

  val enabledCheck = JCheckBox("setEnabled", true)
  enabledCheck.addActionListener {
    button.isEnabled = enabledCheck.isSelected
  }

  val timerCheck = JCheckBox("start", true)
  timerCheck.addActionListener {
    if (timerCheck.isSelected) {
      timer.start()
    } else {
      timer.stop()
    }
  }

  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  button.addHierarchyListener { hierarchyChanged(it, timer) }
  printInfo(button, "after: new JButton, before: add(button); frame.setVisible(true)")

  val panel = JPanel()
  panel.add(button)
  for (i in 0..<15) {
    panel.add(JLabel("<html>JLabel<br>&nbsp;idx:$i"))
  }
  tabs.addTab("Main", JScrollPane(panel))
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JLabel", JLabel("Test"))

  val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
  buttonPanel.add(JLabel("JButton:"))
  buttonPanel.add(visibleCheck)
  buttonPanel.add(enabledCheck)

  val timerPanel = JPanel(FlowLayout(FlowLayout.LEFT))
  timerPanel.add(JLabel("Timer:"))
  timerPanel.add(timerCheck)

  val controls = JPanel(GridLayout(2, 1))
  controls.add(buttonPanel)
  controls.add(timerPanel)
  timer.start()

  return JPanel(BorderLayout()).also {
    it.add(controls, BorderLayout.NORTH)
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun hierarchyChanged(e: HierarchyEvent, timer: Timer) {
  val c = e.component
  val flags = e.changeFlags
  if (flags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
    printInfo(c, "SHOWING_CHANGED")
  }
  if (flags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) {
    printInfo(c, "DISPLAYABILITY_CHANGED")
    // Stop the Timer when the frame is disposed so that it does not
    // keep logging and prevent the application from shutting down.
    if (!c.isDisplayable) {
      timer.stop()
    }
  }
}

fun printInfo(c: Component, message: String) {
  logger.info {
    """
      ${c.javaClass.name}: $message
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
