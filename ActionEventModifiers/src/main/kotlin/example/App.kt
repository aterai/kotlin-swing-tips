package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)

fun makeUI(): Component {
  val textArea = JTextArea()
  logger.useParentHandlers = false
  logger.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))

  val field = JTextField(20)
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      Toolkit.getDefaultToolkit().beep()
    }
  }
  field.actionMap.put("beep", a1)
  field.inputMap.put(KeyStroke.getKeyStroke("shift B"), "beep")
  val listener = object : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      val shiftActive = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK != 0
      if (e.keyCode == KeyEvent.VK_N && shiftActive) {
        Toolkit.getDefaultToolkit().beep()
      }
    }
  }
  field.addKeyListener(listener)

  val button = JButton("TEST: ActionEvent#getModifiers()")
  button.addActionListener { e ->
    // BAD EXAMPLE: val isShiftDown = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0
    // Always use ActionEvent.*_MASK instead of InputEvent.*_MASK in ActionListener
    val isShiftDown = e.modifiers and ActionEvent.SHIFT_MASK != 0
    logger.info { if (isShiftDown) "JButton: Shift is Down" else "JButton: Shift is Up" }
    if (e.modifiers and AWTEvent.MOUSE_EVENT_MASK.toInt() != 0) {
      logger.info { "JButton: Mouse event mask" }
    }
  }

  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(field)
  p.add(button)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater {
      val root = it.rootPane
      root.jMenuBar = makeManuBar()
      root.defaultButton = button
    }
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeManuBar(): JMenuBar {
  val menuBar = JMenuBar()
  val menu = menuBar.add(JMenu("Test"))
  menu.mnemonic = KeyEvent.VK_T

  val a2 = object : AbstractAction("beep") {
    override fun actionPerformed(e: ActionEvent) {
      Toolkit.getDefaultToolkit().beep()
    }
  }
  val item = menu.add(a2)
  item.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.SHIFT_DOWN_MASK)
  item.mnemonic = KeyEvent.VK_I
  item.addActionListener { e ->
    val isShiftDown = e.modifiers and ActionEvent.SHIFT_MASK != 0
    logger.info { if (isShiftDown) "JMenuItem: Shift is Down" else "JMenuItem: Shift is Up" }
    if (e.modifiers and AWTEvent.MOUSE_EVENT_MASK.toInt() != 0) {
      logger.info { "JMenuItem: Mouse event mask" }
    }
  }
  return menuBar
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
