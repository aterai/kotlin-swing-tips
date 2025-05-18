package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*

private val log = JTextArea()

fun makeUI(): Component {
  val button = JButton("open JWindow")
  button.addActionListener { makeWindow(button) }
  val p = JPanel()
  p.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0))
  p.add(button)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeWindow(c: JComponent) {
  val window = JWindow()
  val gc = window.graphicsConfiguration
  if (gc != null && gc.isTranslucencyCapable) {
    window.setBackground(Color(0x0, true))
  }
  val alpha = AtomicInteger(100)
  val animator = Timer(50, null)
  animator.addActionListener {
    val a = alpha.addAndGet(-10)
    if (a < 0) {
      window.dispose()
      animator.stop()
      log.append("JWindow.dispose()\n")
    } else {
      val opacity = a / 100f
      window.opacity = opacity
      log.append("JWindow.setOpacity(%f)%n".format(opacity))
    }
  }
  val shape = RoundRectangle2D.Float(0f, 0f, 240f, 64f, 32f, 32f)
  window.contentPane.add(makePanel(shape, animator))
  window.pack()
  window.setLocationRelativeTo(c.rootPane)
  window.isVisible = true
}

private fun makePanel(shape: Shape, animator: Timer): Component {
  val panel = object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = shape.bounds.size

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as Graphics2D
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = Color(0xAE_3D_9B_CE.toInt(), true)
      g2.fill(shape)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  panel.setOpaque(false)
  panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))
  val dwl = DragWindowListener()
  panel.addMouseListener(dwl)
  panel.addMouseMotionListener(dwl)
  panel.add(makeIconBox())
  panel.add(makeCloseButton(animator), BorderLayout.EAST)
  return panel
}

private fun makeIconBox(): Container {
  val p = JPanel()
  p.setOpaque(false)
  p.add(JLabel(UIManager.getIcon("OptionPane.errorIcon")))
  p.add(JLabel(UIManager.getIcon("OptionPane.questionIcon")))
  p.add(JLabel(UIManager.getIcon("OptionPane.warningIcon")))
  p.add(JLabel(UIManager.getIcon("OptionPane.informationIcon")))
  return p
}

private fun makeCloseButton(animator: Timer): JButton {
  val close = JButton("<html><b>X")
  close.setContentAreaFilled(false)
  close.setBorder(BorderFactory.createEmptyBorder())
  close.setForeground(Color.WHITE)
  close.addActionListener { animator.start() }
  return close
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.getPoint()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.component)
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.location
      c.setLocation(pt.x - startPt.x + e.getX(), pt.y - startPt.y + e.getY())
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
