package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

private const val MW = 300
private const val MH = 200

fun makeUI(): Component {
  val checkbox = JCheckBox("Fixed aspect ratio, Minimum size: $MW*$MH")
  EventQueue.invokeLater {
    val frame = checkbox.topLevelAncestor
    if (frame is Window) {
      frame.minimumSize = Dimension(MW, MH)
      frame.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          if (checkbox.isSelected) {
            initFrameSize(frame)
          }
        }
      })
    }
  }
  checkbox.addActionListener {
    val c = checkbox.topLevelAncestor
    if (c is Window && checkbox.isSelected) {
      initFrameSize(c)
    }
  }

  val label = JLabel()
  label.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      (e.component as? JLabel)?.also {
        val c = it.topLevelAncestor
        if (c is Window) {
          it.text = c.size.toString()
        }
      }
    }
  })

  Toolkit.getDefaultToolkit().setDynamicLayout(false)
  val check = JCheckBox("Toolkit.getDefaultToolkit().setDynamicLayout: ")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    Toolkit.getDefaultToolkit().setDynamicLayout(b)
  }
  val p = JPanel(GridLayout(2, 1))
  p.add(checkbox)
  p.add(check)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

fun initFrameSize(frame: Window) {
  val fw = frame.size.width
  val fh = MH * fw / MW
  frame.setSize(MW.coerceAtLeast(fw), MH.coerceAtLeast(fh))
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
