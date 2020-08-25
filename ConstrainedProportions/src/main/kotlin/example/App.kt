package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val MW = 300
private const val MH = 200

private val checkbox = JCheckBox("Fixed aspect ratio, Minimum size: $MW*$MH")

fun makeUI(): Component {
  EventQueue.invokeLater {
    val frame = checkbox.topLevelAncestor
    if (frame is JFrame) {
      frame.minimumSize = Dimension(MW, MH)
      val cmpListener = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          initFrameSize(frame)
        }
      }
      frame.addComponentListener(cmpListener)
    }
  }
  checkbox.addActionListener {
    val c = checkbox.topLevelAncestor
    if (c is JFrame) {
      initFrameSize(c)
    }
  }

  val label = JLabel()
  val cmpListener = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      (e.component as? JLabel)?.also {
        val c = it.topLevelAncestor
        if (c is JFrame) {
          it.text = c.size.toString()
        }
      }
    }
  }
  label.addComponentListener(cmpListener)

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

fun initFrameSize(frame: JFrame) {
  if (!checkbox.isSelected) {
    return
  }
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
