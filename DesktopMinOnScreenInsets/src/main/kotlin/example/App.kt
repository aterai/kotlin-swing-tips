package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.plaf.basic.BasicInternalFrameUI

private const val OFFSET = 30
private val OPEN_COUNTER = AtomicInteger()

fun makeUI(): Component {
  val f = createInternalFrame()
  val d = (f.ui as? BasicInternalFrameUI)?.northPane?.preferredSize ?: Dimension()
  UIManager.put("Desktop.minOnScreenInsets", Insets(d.height, 16, 3, 16))
  UIManager.put("Desktop.background", Color.LIGHT_GRAY)

  val desktop = JDesktopPane()
  desktop.add(f)

  val menu = JMenu("Window")
  menu.mnemonic = KeyEvent.VK_W
  val menuItem = menu.add("New")
  menuItem.mnemonic = KeyEvent.VK_N
  menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK)
  menuItem.actionCommand = "new"
  menuItem.addActionListener { desktop.add(createInternalFrame()) }
  val menuBar = JMenuBar()
  menuBar.add(menu)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createInternalFrame(): JInternalFrame {
  val title = "Document #${OPEN_COUNTER.getAndIncrement()}"
  val f = JInternalFrame(title, true, true, true, true)
  f.contentPane.add(JScrollPane(JTree()))
  f.setSize(160, 100)
  f.setLocation(OFFSET * OPEN_COUNTER.toInt(), OFFSET * OPEN_COUNTER.toInt())
  EventQueue.invokeLater { f.isVisible = true }
  return f
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
