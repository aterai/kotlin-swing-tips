package example

import java.awt.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.text.DefaultCaret

private val check = JCheckBox("on EDT", true)
private val start = JButton("Start")
private val stop = JButton("Stop")
private val textArea0 = JTextArea()
private val textArea1 = JTextArea()
private val textArea2 = JTextArea()
private var worker: SwingWorker<String, String>? = null

fun makeUI(): Component {
  (textArea0.caret as? DefaultCaret)?.updatePolicy = DefaultCaret.UPDATE_WHEN_ON_EDT // default
  (textArea1.caret as? DefaultCaret)?.updatePolicy = DefaultCaret.ALWAYS_UPDATE
  (textArea2.caret as? DefaultCaret)?.updatePolicy = DefaultCaret.NEVER_UPDATE

  val p = JPanel(GridLayout(1, 0))
  p.add(makeTitledPanel("UPDATE_WHEN_ON_EDT", JScrollPane(textArea0)))
  p.add(makeTitledPanel("ALWAYS_UPDATE", JScrollPane(textArea1)))
  p.add(makeTitledPanel("NEVER_UPDATE", JScrollPane(textArea2)))

  for (i in 0 until 10) {
    test(i.toString())
  }
  start.addActionListener { startTest() }
  stop.isEnabled = false
  stop.addActionListener {
    worker?.cancel(true)
    worker = null
  }
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(Box.createHorizontalGlue())
  box.add(check)
  box.add(Box.createHorizontalStrut(5))
  box.add(start)
  box.add(Box.createHorizontalStrut(5))
  box.add(stop)
  return JPanel().also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask : SwingWorker<String, String>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    while (!isCancelled) {
      Thread.sleep(500)
      if (check.isSelected) {
        publish(LocalDateTime.now(ZoneId.systemDefault()).toString()) // On EDT
      } else {
        test(LocalDateTime.now(ZoneId.systemDefault()).toString()) // Not on EDT
      }
    }
    return "Cancelled"
  }
}

private fun test(s: String) {
  insertText(textArea0, s)
  insertText(textArea1, s)
  insertText(textArea2, s)
}

private fun startTest() {
  if (worker == null) {
    worker = object : BackgroundTask() {
      override fun process(chunks: List<String>) {
        chunks.forEach { test(it) }
      }

      override fun done() {
        check.isEnabled = true
        start.isEnabled = true
        stop.isEnabled = false
      }
    }.also {
      check.isEnabled = false
      start.isEnabled = false
      stop.isEnabled = true
      it.execute()
    }
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun insertText(
  textArea: JTextArea,
  s: String,
) {
  textArea.append("$s\n")
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
