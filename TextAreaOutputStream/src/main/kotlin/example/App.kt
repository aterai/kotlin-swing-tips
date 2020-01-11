package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  companion object {
    val LOGGER_NAME: String = MethodHandles.lookup().lookupClass().name
    private val LOGGER = Logger.getLogger(LOGGER_NAME)
  }

  init {
    val textArea = JTextArea()
    // TEST: textArea.getDocument().addDocumentListener(FIFODocumentListener(textArea))
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
    add(JScrollPane(textArea))
    add(box, BorderLayout.SOUTH)
    preferredSize = Dimension(320, 240)
  }
}

class EnterAction(private val textField: JTextField) : AbstractAction("Enter") {
  override fun actionPerformed(e: ActionEvent) {
    LOGGER.info { "%s%n  %s%n".format(LocalDateTime.now(ZoneId.systemDefault()), textField.text) }
  }

  companion object {
    private val LOGGER = Logger.getLogger(MainPanel.LOGGER_NAME)
  }
}

class TextAreaOutputStream(private val textArea: JTextArea) : OutputStream() {
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

class TextAreaHandler(os: OutputStream) : StreamHandler() {
  private fun configure() {
    formatter = SimpleFormatter()
    runCatching {
      encoding = "UTF-8"
    }
  }

  @Synchronized
  override fun publish(record: LogRecord) {
    super.publish(record)
    flush()
  }

  @Synchronized
  override fun close() {
    flush()
  }

  init {
    configure()
    setOutputStream(os)
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
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
