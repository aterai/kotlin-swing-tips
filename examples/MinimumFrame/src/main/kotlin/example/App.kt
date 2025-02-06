package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

private const val MW = 320
private const val MH1 = 100
private const val MH2 = 150

fun makeUI(): Component {
  val label = JLabel()
  val cl = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      val w = SwingUtilities.getWindowAncestor(label.rootPane)
      (e.component as? JLabel)?.text = w.size.toString()
    }
  }
  label.addComponentListener(cl)

  val title1 = "The minimum size of this window: ${MW}x$MH1"
  val check1 = JCheckBox(title1, true)
  check1.addActionListener { e ->
    val c = e.source
    if (c is JCheckBox && c.isSelected) {
      initFrameSize(SwingUtilities.getWindowAncestor(label.rootPane))
    }
  }

  val title2 = "The minimum size of this window(since 1.6): ${MW}x$MH2"
  val check2 = JCheckBox(title2, true)
  check2.addActionListener { e ->
    val w = SwingUtilities.getWindowAncestor(label.rootPane)
    val b = (e.source as? JCheckBox)?.isSelected == true
    w.minimumSize = if (b) Dimension(MW, MH2) else null
  }

  EventQueue.invokeLater {
    val w = SwingUtilities.getWindowAncestor(label.rootPane)
    w.minimumSize = Dimension(MW, MH2)
    val cl2 = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        val win = e.component
        if (check1.isSelected && win is Window) {
          initFrameSize(win)
        }
      }
    }
    w.addComponentListener(cl2)
  }
  val box = Box.createVerticalBox()
  box.add(check1)
  box.add(check2)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

fun initFrameSize(frame: Window) {
  val fw = frame.size.width
  val fh = frame.size.height
  frame.setSize(MW.coerceAtLeast(fw), MH1.coerceAtLeast(fh))
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
