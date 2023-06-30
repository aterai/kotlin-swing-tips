package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseMotionListener
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.plaf.basic.BasicInternalFrameUI

private const val OFFSET = 30
private val openFrameCount = AtomicInteger()
private val desktop = JDesktopPane()

fun makeUI(): Component {
  val immovableFrame = JInternalFrame("immovable", false, false, true, true)
  (immovableFrame.ui as? BasicInternalFrameUI)?.northPane?.also {
    val actions = it.getListeners(MouseMotionListener::class.java)
    for (l in actions) {
      it.removeMouseMotionListener(l)
    }
  }
  immovableFrame.setSize(160, 0)
  desktop.add(immovableFrame)
  immovableFrame.isVisible = true
  desktop.dragMode = JDesktopPane.OUTLINE_DRAG_MODE
  val handler = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      immovableFrame.setSize(immovableFrame.size.width, e.component.size.height)
    }
  }
  desktop.addComponentListener(handler)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar(): JMenuBar {
  val menu = JMenu("Window")
  menu.mnemonic = KeyEvent.VK_W
  val menuItem = menu.add("New")
  menuItem.mnemonic = KeyEvent.VK_N
  menuItem.accelerator = KeyStroke.getKeyStroke(
    KeyEvent.VK_N,
    InputEvent.ALT_DOWN_MASK
  )
  menuItem.actionCommand = "new"
  menuItem.addActionListener {
    val frame = createInternalFrame()
    desktop.add(frame)
    frame.isVisible = true
  }
  val menuBar = JMenuBar()
  menuBar.add(menu)
  return menuBar
}

private fun createInternalFrame(): JInternalFrame {
  val title = "Document #${openFrameCount.getAndIncrement()}"
  val f = JInternalFrame(title, true, true, true, true)
  f.setSize(160, 100)
  f.setLocation(OFFSET * openFrameCount.toInt(), OFFSET * openFrameCount.toInt())
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
