package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  for (i in 1 until 100) {
    tabs.addTab("title$i", JLabel("label$i"))
  }
  val am = tabs.actionMap
  val forward = "scrollTabsForwardAction"
  val backward = "scrollTabsBackwardAction"
  var forwardButton: JButton? = null
  var backwardButton: JButton? = null
  for (c in tabs.components) {
    if (c is JButton) {
      if (forwardButton == null) {
        forwardButton = c
        addRepeatHandler(forwardButton, am[forward])
      } else if (backwardButton == null) {
        backwardButton = c
        addRepeatHandler(backwardButton, am[backward])
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addRepeatHandler(
  button: JButton,
  action: Action,
) {
  val handler = ActionRepeatHandler(action)
  button.addActionListener(handler)
  button.addMouseListener(handler)
}

private class ActionRepeatHandler(
  private val action: Action,
) : MouseAdapter(), ActionListener {
  private val timer: Timer
  private var button: JButton? = null

  init {
    timer = Timer(60, this)
    timer.initialDelay = 300
  }

  override fun actionPerformed(e: ActionEvent) {
    val o = e.source
    val m = button?.model
    if (o is Timer && m != null) {
      if (!m.isPressed && timer.isRunning) {
        timer.stop()
        button = null
      } else {
        val c = SwingUtilities.getAncestorOfClass(JTabbedPane::class.java, button)
        action.actionPerformed(
          ActionEvent(
            c,
            ActionEvent.ACTION_PERFORMED,
            null,
            e.getWhen(),
            e.modifiers,
          ),
        )
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && e.component.isEnabled) {
      button = e.component as? JButton
      timer.start()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    timer.stop()
    button = null
  }

  override fun mouseExited(e: MouseEvent) {
    if (timer.isRunning) {
      timer.stop()
    }
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
