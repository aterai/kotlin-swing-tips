package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val label = object : JLabel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      val w = width.toFloat()
      val h = height.toFloat()
      g2.paint = GradientPaint(0f, 0f, Color.ORANGE, w, h, Color.WHITE, true)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }

    override fun getPreferredSize() = Dimension(640, 640)
  }
  label.border = BorderFactory.createTitledBorder("Horizontal scroll: CTRL + Wheel")
  label.addMouseWheelListener { e ->
    val c = e.component
    val sp = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, c)
    if (sp is JScrollPane) {
      val sb = if (e.isControlDown) sp.horizontalScrollBar else sp.verticalScrollBar
      sb.dispatchEvent(SwingUtilities.convertMouseEvent(c, e, sb))
    }
  }
  val scroll = JScrollPane(label)
  scroll.verticalScrollBar.unitIncrement = 10

  val hsb = scroll.horizontalScrollBar
  hsb.unitIncrement = 10
  hsb.addMouseWheelListener { e ->
    val c = e.component
    val sp = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, c)
    if (sp is JScrollPane) {
      val viewport = sp.viewport
      val vp = viewport.viewPosition
      val d = hsb.unitIncrement * e.wheelRotation
      vp.translate(d, 0)
      val view = SwingUtilities.getUnwrappedView(viewport)
      (view as? JComponent)?.scrollRectToVisible(Rectangle(vp, viewport.size))
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
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
