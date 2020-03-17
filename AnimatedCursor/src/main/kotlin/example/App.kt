package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val pt = Point()
  val cl = Thread.currentThread().contextClassLoader
  val tk = Toolkit.getDefaultToolkit()
  val list = listOf("00", "01", "02").map {
    tk.createCustomCursor(tk.createImage(cl.getResource("example/$it.png")), pt, it)
  }

  val animator = Timer(100, null)
  val button = JButton("Start")
  button.cursor = list[0]
  button.addActionListener { e ->
    val b = e.source as? JButton ?: return@addActionListener
    if (animator.isRunning) {
      b.text = "Start"
      animator.stop()
    } else {
      b.text = "Stop"
      animator.start()
    }
  }
  button.addHierarchyListener { e ->
    if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L && !e.component.isDisplayable) {
      animator.stop()
    }
  }
  animator.addActionListener(CursorActionListener(button, list))

  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createEmptyBorder(32, 32, 32, 32)
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createTitledBorder("delay=100ms")
    it.preferredSize = Dimension(320, 240)
  }
}

private class CursorActionListener(
  private val comp: Component,
  private val frames: List<Cursor>
) : ActionListener {
  private var counter = 0
  override fun actionPerformed(e: ActionEvent) {
    comp.cursor = frames[counter]
    counter = (counter + 1) % frames.size
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
