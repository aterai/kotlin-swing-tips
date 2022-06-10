package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.AWTEventListener
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val DELAY = 10 * 1000 // 10s
private val label = JLabel("Not connected")
private val combo = JComboBox(makeComboBoxModel())
private val textField = JTextField(20)
private val button = JButton("Connect")

fun makeUI(): Component {
  val timer = Timer(DELAY, null)
  val awtEvent = AWTEventListener {
    if (timer.isRunning) {
      // println("timer.restart()")
      timer.restart()
    }
  }
  timer.addActionListener { e ->
    // println("timeout")
    setTestConnected(false)
    Toolkit.getDefaultToolkit().removeAWTEventListener(awtEvent)
    (e.source as? Timer)?.stop()
  }
  button.addActionListener {
    setTestConnected(true)
    val tk = Toolkit.getDefaultToolkit()
    tk.addAWTEventListener(awtEvent, AWTEvent.KEY_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK)
    timer.isRepeats = false
    timer.start()
  }
  setTestConnected(false)

  val p = JPanel(BorderLayout())
  p.add(label)
  p.add(button, BorderLayout.EAST)

  val panel = JPanel(BorderLayout(5, 5))
  panel.border = BorderFactory.createTitledBorder("Dummy")

  val box = JPanel(BorderLayout(5, 5))
  box.add(textField)
  box.add(combo, BorderLayout.EAST)
  panel.add(box, BorderLayout.NORTH)
  panel.add(JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(panel)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setTestConnected(flag: Boolean) {
  val str = if (flag) "<font color='blue'>Connected" else "<font color='red'>Not connected"
  label.text = "<html>Status: $str"
  combo.isEnabled = flag
  textField.isEnabled = flag
  button.isEnabled = !flag
}

private fun makeComboBoxModel() = DefaultComboBoxModel<String>().also {
  it.addElement("dummy model")
  it.addElement("123456")
  it.addElement("0987654321")
  it.addElement("41234123")
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
