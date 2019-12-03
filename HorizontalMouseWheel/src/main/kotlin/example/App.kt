package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val label: JLabel = object : JLabel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      val w = getWidth().toFloat()
      val h = getHeight().toFloat()
      g2.setPaint(GradientPaint(0f, 0f, Color.ORANGE, w, h, Color.WHITE, true))
      g2.fillRect(0, 0, getWidth(), getHeight())
      g2.dispose()
    }

    override fun getPreferredSize() = Dimension(640, 640)
  }
  label.setBorder(BorderFactory.createTitledBorder("Horizontal scroll: CTRL + Wheel"))
  label.addMouseWheelListener { e ->
    val c = e.getComponent()
    (SwingUtilities.getAncestorOfClass(JScrollPane::class.java, c) as? JScrollPane)?.also {
      val sb = if (e.isControlDown()) it.getHorizontalScrollBar() else it.getVerticalScrollBar()
      sb.dispatchEvent(SwingUtilities.convertMouseEvent(c, e, sb))
    }
  }
  val scroll = JScrollPane(label)
  scroll.getVerticalScrollBar().setUnitIncrement(10)
  val hsb = scroll.getHorizontalScrollBar()
  hsb.setUnitIncrement(10)
  hsb.addMouseWheelListener { e ->
    val sb = e.getComponent() as? JScrollBar ?: return@addMouseWheelListener
    (SwingUtilities.getAncestorOfClass(JScrollPane::class.java, sb) as? JScrollPane)?.also {
      val vport = it.getViewport()
      val vp = vport.getViewPosition()
      val d = hsb.getUnitIncrement() * e.getWheelRotation()
      vp.translate(d, 0)
      (SwingUtilities.getUnwrappedView(vport) as? JComponent)?.scrollRectToVisible(Rectangle(vp, vport.getSize()))
    }
  }
  val p = JPanel(BorderLayout())
  p.add(scroll)
  p.setPreferredSize(Dimension(320, 240))
  return p
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
