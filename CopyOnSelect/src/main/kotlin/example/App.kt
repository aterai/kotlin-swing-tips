package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.CaretEvent
import javax.swing.event.CaretListener
import javax.swing.text.JTextComponent

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    val log = JTextArea()
    LOGGER.setUseParentHandlers(false)
    LOGGER.addHandler(TextAreaHandler(TextAreaOutputStream(log)))
    val textArea = object : JTextArea("012345 67890 123456789") {
      @Transient
      private var handler: CopyOnSelectListener? = null

      override fun updateUI() {
        removeCaretListener(handler)
        removeMouseListener(handler)
        removeKeyListener(handler)
        super.updateUI()
        handler = CopyOnSelectListener()
        addCaretListener(handler)
        addMouseListener(handler)
        addKeyListener(handler)
      }
    }
    add(makeTitledPanel("Copy On Select", JScrollPane(textArea)))
    add(makeTitledPanel("log", JScrollPane(log)))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
}

  companion object {
    val LOGGER_NAME: String = MethodHandles.lookup().lookupClass().getName()
    private val LOGGER = Logger.getLogger(LOGGER_NAME)
  }
}

class CopyOnSelectListener : MouseAdapter(), CaretListener, KeyListener {
  private var dragActive = false
  private var shiftActive = false
  private var dot = 0
  private var mark = 0
  override fun caretUpdate(e: CaretEvent) {
    if (!dragActive && !shiftActive) {
      fire(e.getSource())
    }
  }

  override fun mousePressed(e: MouseEvent) {
    dragActive = true
  }

  override fun mouseReleased(e: MouseEvent) {
    dragActive = false
    fire(e.getSource())
}

  override fun keyPressed(e: KeyEvent) {
    shiftActive = e.getModifiersEx() and InputEvent.SHIFT_DOWN_MASK != 0
}

  override fun keyReleased(e: KeyEvent) {
    shiftActive = e.getModifiersEx() and InputEvent.SHIFT_DOWN_MASK != 0
    if (!shiftActive) {
      fire(e.getSource())
    }
  }

  override fun keyTyped(e: KeyEvent) {
    /* empty */
  }

  private fun fire(c: Any) {
    if (c is JTextComponent) {
      val caret = c.getCaret()
      val d = caret.getDot()
      val m = caret.getMark()
      if (d != m && (dot != d || mark != m)) {
        c.getSelectedText()?.also {
          LOGGER.info { it }
          c.copy()
      }
    }
      dot = d
      mark = m
    }
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
    setFormatter(SimpleFormatter())
    runCatching {
      setEncoding("UTF-8")
    }.onFailure {
      // doing a setEncoding with null should always work.
      setEncoding(null)
    }
  }

  @Synchronized
  override fun publish(record: LogRecord?) {
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
