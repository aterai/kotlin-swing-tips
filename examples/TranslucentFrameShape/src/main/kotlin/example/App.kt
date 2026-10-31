package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun createUI(): Component {
  val shape = RoundRectangle2D.Float(0f, 0f, 240f, 64f, 32f, 32f)
  val button1 = createButton("use Window#setShape(...)", shape, true)
  val button2 = createButton("not use Window#setShape(...)", shape, false)

  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
  p.add(button1)
  p.add(button2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createButton(
  title: String,
  shape: Shape,
  useSetShape: Boolean,
): JButton {
  val button = JButton(title)
  button.addActionListener { e ->
    val c = (e.source as? AbstractButton)?.rootPane
    showWindow(c, shape, useSetShape)
  }
  return button
}

private fun showWindow(
  parent: Component?,
  shape: Shape,
  useSetShape: Boolean,
) {
  val window = JWindow()
  setTranslucencyBackground(window)
  window.contentPane.add(createPanel(shape))
  if (useSetShape) {
    // Hard clipping: the edges of the window become jagged
    window.shape = shape
  }
  window.pack()
  window.setLocationRelativeTo(parent)
  window.isVisible = true
}

private fun setTranslucencyBackground(window: JWindow) {
  val gc = window.graphicsConfiguration
  if (gc?.isTranslucencyCapable == true) {
    window.background = Color(0x0, true)
  }
}

private fun createPanel(shape: Shape): Component {
  val panel = object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = shape.bounds.size

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC, .5f)
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = Color.RED
      g2.fill(shape)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  panel.isOpaque = false
  panel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
  val dwl = DragWindowListener()
  panel.addMouseListener(dwl)
  panel.addMouseMotionListener(dwl)
  panel.add(createIconBox())
  panel.add(createCloseButton(), BorderLayout.EAST)
  return panel
}

private fun createIconBox(): Container {
  val p = JPanel()
  p.isOpaque = false
  p.add(JLabel(UIManager.getIcon("OptionPane.errorIcon")))
  p.add(JLabel(UIManager.getIcon("OptionPane.questionIcon")))
  p.add(JLabel(UIManager.getIcon("OptionPane.warningIcon")))
  p.add(JLabel(UIManager.getIcon("OptionPane.informationIcon")))
  return p
}

private fun createCloseButton(): JButton {
  val close = JButton("<html><b>X")
  close.isContentAreaFilled = false
  close.border = BorderFactory.createEmptyBorder()
  close.foreground = Color.WHITE
  close.addActionListener { e ->
    (e.source as? Component)?.also {
      SwingUtilities.getWindowAncestor(it)?.dispose()
    }
  }
  return close
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
