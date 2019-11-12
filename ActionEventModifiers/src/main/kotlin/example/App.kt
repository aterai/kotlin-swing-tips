package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName())

  init {
    val textArea = JTextArea()
    logger.setUseParentHandlers(false)
    logger.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))
    val field = JTextField(20)
    field.getActionMap().put("beep", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent?) {
        Toolkit.getDefaultToolkit().beep()
      }
    })
    field.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK), "beep")
    field.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val shiftActive = e.getModifiersEx() and InputEvent.SHIFT_DOWN_MASK != 0
        if (e.getKeyCode() == KeyEvent.VK_N && shiftActive) {
          Toolkit.getDefaultToolkit().beep()
        }
      }
    })
    val button = JButton("TEST: ActionEvent#getModifiers()")
    button.addActionListener { e ->
      // BAD EXAMPLE: val isShiftDown = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0
      // Always use ActionEvent.*_MASK instead of InputEvent.*_MASK in ActionListener
      val isShiftDown = e.getModifiers() and ActionEvent.SHIFT_MASK != 0
      logger.info { if (isShiftDown) "JButton: Shift is Down" else "JButton: Shift is Up" }
      if (e.getModifiers() and AWTEvent.MOUSE_EVENT_MASK.toInt() != 0) {
        logger.info { "JButton: Mouse event mask" }
      }
    }
    val menuBar = JMenuBar()
    val menu = menuBar.add(JMenu("Test"))
    menu.setMnemonic(KeyEvent.VK_T)
    val item = menu.add(object : AbstractAction("beep") {
      override fun actionPerformed(e: ActionEvent) {
        Toolkit.getDefaultToolkit().beep()
      }
    })
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.SHIFT_DOWN_MASK))
    item.setMnemonic(KeyEvent.VK_I)
    item.addActionListener { e ->
      val isShiftDown = e.getModifiers() and ActionEvent.SHIFT_MASK != 0
      logger.info { if (isShiftDown) "JMenuItem: Shift is Down" else "JMenuItem: Shift is Up" }
      if (e.getModifiers() and AWTEvent.MOUSE_EVENT_MASK.toInt() != 0) {
        logger.info { "JMenuItem: Mouse event mask" }
      }
    }
    EventQueue.invokeLater {
      val root = getRootPane()
      root.setJMenuBar(menuBar)
      root.setDefaultButton(button)
    }
    val p = JPanel(GridLayout(2, 1, 5, 5))
    p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    p.add(field)
    p.add(button)
    add(p, BorderLayout.NORTH)
    add(JScrollPane(textArea))
    setPreferredSize(Dimension(320, 240))
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
}

class TextAreaHandler(os: OutputStream) : StreamHandler() {
  private fun configure() {
    setFormatter(SimpleFormatter())
    runCatching {
      setEncoding("UTF-8")
    }.onFailure {
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
