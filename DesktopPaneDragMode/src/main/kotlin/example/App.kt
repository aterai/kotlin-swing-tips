package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val OFFSET = 30
private val OPEN_FRAME_COUNTER = AtomicInteger()

fun makeUI(): Component {
  val desktop = JDesktopPane()

  val r1 = JRadioButton("LIVE_DRAG_MODE", true)
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      desktop.dragMode = JDesktopPane.LIVE_DRAG_MODE
    }
  }

  val r2 = JRadioButton("OUTLINE_DRAG_MODE")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      desktop.dragMode = JDesktopPane.OUTLINE_DRAG_MODE
    }
  }

  val p = JPanel()
  val bg = ButtonGroup()
  listOf(r1, r2).forEach {
    bg.add(it)
    p.add(it)
  }

  val menu = JMenu("Window")
  menu.mnemonic = KeyEvent.VK_W

  val menuItem = menu.add("New")
  menuItem.mnemonic = KeyEvent.VK_N
  menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK)
  menuItem.actionCommand = "new"
  menuItem.addActionListener {
    val frame = createInternalFrame()
    desktop.add(frame)
    frame.isVisible = true
  }

  val menuBar = JMenuBar()
  menuBar.add(menu)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(desktop)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createInternalFrame(): JInternalFrame {
  val title = "Document #${OPEN_FRAME_COUNTER.getAndIncrement()}"
  val f = JInternalFrame(title, true, true, true, true)
  f.contentPane.add(JScrollPane(JTree()))
  f.setSize(160, 100)
  f.setLocation(OFFSET * OPEN_FRAME_COUNTER.toInt(), OFFSET * OPEN_FRAME_COUNTER.toInt())
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
