package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
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
  textArea.isEditable = false
  logger.useParentHandlers = false
  logger.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))

  val p = JPanel(BorderLayout())
  p.isFocusable = true
  val ml = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount >= 2) {
        toggleFullScreenWindow(p)
      }
    }
  }
  p.addMouseListener(ml)

  val key = "full-screen"
  p.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), key)
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      toggleFullScreenWindow(p)
    }
  }
  p.actionMap.put(key, a1)

  p.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close")
  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      logger.info { "ESC KeyEvent:" }
      (p.topLevelAncestor as? Window)?.also {
        it.dispatchEvent(WindowEvent(it, WindowEvent.WINDOW_CLOSING))
      }
    }
  }
  p.actionMap.put("close", a2)

  val label = JLabel("<html>F11 or Double Click: toggle full-screen<br>ESC: exit")
  label.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  p.add(label, BorderLayout.NORTH)
  p.add(JScrollPane(textArea))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

fun toggleFullScreenWindow(c: JComponent) {
  (c.topLevelAncestor as? Dialog)?.also {
    val graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    if (graphicsDevice.fullScreenWindow == null) {
      it.dispose() // destroy the native resources
      it.isUndecorated = true
      it.isVisible = true // rebuilding the native resources
      graphicsDevice.fullScreenWindow = it
    } else {
      graphicsDevice.fullScreenWindow = null
      it.dispose()
      it.isUndecorated = false
      it.isVisible = true
      it.repaint()
    }
  }
  c.requestFocusInWindow() // for Ubuntu
}

private class TextAreaOutputStream(
  private val textArea: JTextArea,
) : OutputStream() {
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

private class TextAreaHandler(
  os: OutputStream,
) : StreamHandler(os, SimpleFormatter()) {
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
    val wl = object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        logger.info { "windowClosing:" }
        logger.info { "  triggered only when you click on the X" }
        logger.info { "  or on the close menu item in the window's system menu.'" }
      }

      override fun windowClosed(e: WindowEvent) {
        logger.info { "windowClosed & rebuild:" }
      }
    }
    JDialog().apply {
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      addWindowListener(wl)
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
