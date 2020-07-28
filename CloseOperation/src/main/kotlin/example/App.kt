package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

val COUNTER = AtomicInteger(0)

fun makeUI(): Component {
  val button = JButton("New Frame")
  button.addActionListener { e ->
    val f = createFrame(null)
    f.contentPane.add(makeUI())
    f.pack()

    val c = (e.source as? JComponent)?.topLevelAncestor
    if (c is Window) {
      val pt = c.getLocation()
      f.setLocation(pt.x, pt.y + f.size.height)
    }
    f.isVisible = true
  }

  return JPanel(BorderLayout()).also {
    it.add(button)
    it.preferredSize = Dimension(320, 100)
  }
}

fun createFrame(title: String?): JFrame {
  val frame = JFrame(title ?: "Frame #$COUNTER")
  frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
  COUNTER.getAndIncrement()
  val wl = object : WindowAdapter() {
    override fun windowClosing(e: WindowEvent) {
      if (COUNTER.getAndDecrement() == 0) {
        val w = e.window
        if (w is JFrame) {
          w.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }
      }
    }
  }
  frame.addWindowListener(wl)
  return frame
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
