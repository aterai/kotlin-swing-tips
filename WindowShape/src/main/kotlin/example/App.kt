package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import javax.swing.*

private val FRC = FontRenderContext(null, true, true)
private val FONT = Font(Font.SERIF, Font.PLAIN, 300)

fun makeUI(): Component {
  val label = JLabel("", SwingConstants.CENTER)
  val frame = JFrame().also {
    it.isUndecorated = true
    it.isAlwaysOnTop = true
    it.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
    it.contentPane.add(label)
    it.contentPane.background = Color.GREEN
    it.pack()
  }
  val textField = JTextField("š", 20)
  val button = JToggleButton("show")
  button.addActionListener { e ->
    val btn = e.source as? AbstractButton
    if (btn?.isSelected == true) {
      val str = textField.text.trim()
      val tl = TextLayout(str, FONT, FRC)
      val b = tl.bounds
      val shape = tl.getOutline(AffineTransform.getTranslateInstance(-b.x, -b.y))
      frame.bounds = shape.bounds
      frame.shape = shape
      frame.setLocationRelativeTo(btn.rootPane)
      frame.isVisible = true
    } else {
      frame.isVisible = false
    }
  }
  return JPanel().also {
    it.add(textField)
    it.add(button)
    val dwl = DragWindowListener()
    label.addMouseListener(dwl)
    label.addMouseMotionListener(dwl)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.component)
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.location
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
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
