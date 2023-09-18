package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.nio.charset.StandardCharsets
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import javax.swing.*
import javax.swing.event.CaretEvent
import javax.swing.event.CaretListener
import javax.swing.text.JTextComponent

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)

fun makeUI(): Component {
  val log = JTextArea()
  logger.useParentHandlers = false
  logger.addHandler(TextAreaHandler(TextAreaOutputStream(log)))
  val textArea = object : JTextArea("012345 67890 123456789") {
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
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Copy On Select", JScrollPane(textArea)))
    it.add(makeTitledPanel("log", JScrollPane(log)))
    it.preferredSize = Dimension(320, 240)
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

private class CopyOnSelectListener : MouseAdapter(), CaretListener, KeyListener {
  private var dragActive = false
  private var shiftActive = false
  private var dot = 0
  private var mark = 0

  override fun caretUpdate(e: CaretEvent) {
    if (!dragActive && !shiftActive) {
      fire(e.source)
    }
  }

  override fun mousePressed(e: MouseEvent) {
    dragActive = true
  }

  override fun mouseReleased(e: MouseEvent) {
    dragActive = false
    fire(e.source)
  }

  override fun keyPressed(e: KeyEvent) {
    shiftActive = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK != 0
  }

  override fun keyReleased(e: KeyEvent) {
    shiftActive = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK != 0
    if (!shiftActive) {
      fire(e.source)
    }
  }

  override fun keyTyped(e: KeyEvent) {
    // empty
  }

  private fun fire(c: Any) {
    if (c is JTextComponent) {
      val caret = c.caret
      val d = caret.dot
      val m = caret.mark
      if (d != m && (dot != d || mark != m)) {
        c.selectedText?.also {
          logger.info { it }
          c.copy()
        }
      }
      dot = d
      mark = m
    }
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

  override fun write(
    b: ByteArray,
    off: Int,
    len: Int,
  ) {
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
