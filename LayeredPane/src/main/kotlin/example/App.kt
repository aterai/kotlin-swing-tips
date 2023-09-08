package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*

private val desktop = JDesktopPane()

fun makeUI(): Component {
  val frame = JInternalFrame("AlwaysOnTop", true, false, true, true)
  frame.setSize(180, 180)
  val layer = JLayeredPane.MODAL_LAYER + 1
  val position = 0
  desktop.add(frame, layer, position)
  EventQueue.invokeLater {
    frame.isVisible = true
    desktop.rootPane.jMenuBar = createMenuBar()
  }
  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar(): JMenuBar {
  val menuBar = JMenuBar()
  val menu = JMenu("Document")
  menu.mnemonic = KeyEvent.VK_D
  menuBar.add(menu)
  var menuItem = menu.add("New")
  menuItem.mnemonic = KeyEvent.VK_N
  menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK)
  menuItem.actionCommand = "new"
  menuItem.addActionListener {
    val frame = MyInternalFrame()
    desktop.add(frame)
    frame.isVisible = true
  }
  menu.add(menuItem)
  menuItem = menu.add("Quit")
  menuItem.mnemonic = KeyEvent.VK_Q
  menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK)
  menuItem.actionCommand = "quit"
  menuItem.addActionListener {
    SwingUtilities.getWindowAncestor(desktop)?.dispose()
  }
  menu.add(menuItem)
  return menuBar
}

private class MyInternalFrame : JInternalFrame(
  "Document #%s".format(OPEN_FRAME_COUNT.getAndIncrement()),
  true,
  true,
  true,
  true,
) {
  init {
    setSize(180, 100)
    setLocation(OFFSET * OPEN_FRAME_COUNT.toInt(), OFFSET * OPEN_FRAME_COUNT.toInt())
  }

  companion object {
    private const val OFFSET = 30
    private val OPEN_FRAME_COUNT = AtomicInteger()
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
