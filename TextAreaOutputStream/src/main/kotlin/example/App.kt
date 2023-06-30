package example

import java.awt.*
import java.awt.event.ActionEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import javax.swing.*

private val LOGGER_NAME = MethodHandles.lookup().lookupClass().name
private val LOGGER = Logger.getLogger(LOGGER_NAME)

fun makeUI(): Component {
  val textArea = JTextArea()
  textArea.isEditable = false

  LOGGER.useParentHandlers = false
  LOGGER.level = Level.ALL
  LOGGER.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))
  LOGGER.info { "test, TEST" }

  val button = JButton("Clear")
  button.addActionListener { textArea.text = "" }

  val textField = JTextField("aaa")

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(Box.createHorizontalGlue())
  box.add(textField)
  box.add(Box.createHorizontalStrut(5))
  box.add(JButton(EnterAction(textField)))
  box.add(Box.createHorizontalStrut(5))
  box.add(button)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class EnterAction(private val textField: JTextField) : AbstractAction("Enter") {
  override fun actionPerformed(e: ActionEvent) {
    LOGGER.info { "%s%n  %s%n".format(LocalDateTime.now(ZoneId.systemDefault()), textField.text) }
  }

  companion object {
    private val LOGGER = Logger.getLogger(LOGGER_NAME)
  }
}

private class TextAreaOutputStream(private val textArea: JTextArea) : OutputStream() {
  private val buffer = ByteArrayOutputStream()

  @Throws(IOException::class)
  override fun flush() {
    textArea.append(buffer.toString("UTF-8"))
    buffer.reset()
  }

  override fun write(b: Int) {
    buffer.write(b)
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    buffer.write(b, off, len)
  }
}

private class TextAreaHandler(os: OutputStream) : StreamHandler(os, SimpleFormatter()) {
  override fun getEncoding() = StandardCharsets.UTF_8.name()

  @Synchronized
  override fun publish(logRecord: LogRecord) {
    super.publish(logRecord)
    flush()
  }

  @Synchronized
  override fun close() {
    flush()
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
