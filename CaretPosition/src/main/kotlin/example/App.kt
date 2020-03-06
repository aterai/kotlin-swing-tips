package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val LS = "\n"
private const val LIMIT = 1000
private val timer = Timer(200, null)
private val jtp = JTextPane()
private val startButton = JButton("Start")
private val stopButton = JButton("Stop")

private fun timerStop() {
  timer.stop()
  startButton.isEnabled = true
  stopButton.isEnabled = false
}

private fun timerStart() {
  startButton.isEnabled = false
  stopButton.isEnabled = true
  timer.start()
}

private fun append(str: String) {
  val doc = jtp.document
  val text = if (doc.length > LIMIT) {
    timerStop()
    startButton.isEnabled = false
    "doc.getLength()>1000"
  } else {
    str
  }
  runCatching {
    doc.insertString(doc.length, text + LS, null)
    jtp.caretPosition = doc.length
  }
}

fun makeUI(): Component {
  jtp.isEditable = false
  timer.addActionListener {
    append(LocalDateTime.now(ZoneId.systemDefault()).toString())
  }
  startButton.addActionListener { timerStart() }
  stopButton.addActionListener { timerStop() }
  stopButton.isEnabled = false

  val clearButton = JButton("Clear")
  clearButton.addActionListener {
    jtp.text = ""
    if (!timer.isRunning) {
      startButton.isEnabled = true
      stopButton.isEnabled = false
    }
  }

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(Box.createHorizontalGlue())
  box.add(startButton)
  box.add(stopButton)
  box.add(Box.createHorizontalStrut(5))
  box.add(clearButton)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(jtp))
    it.add(box, BorderLayout.SOUTH)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
