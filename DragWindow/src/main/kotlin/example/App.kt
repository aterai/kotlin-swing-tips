package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import javax.imageio.ImageIO
import javax.swing.*

fun start(frame: JFrame) {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/splash.png")
  val image = url?.openStream()?.use(ImageIO::read)
  val icon = image?.let { ImageIcon(it) } ?: MissingIcon()
  val splashScreen = createSplashScreen(frame, icon)
  splashScreen.isVisible = true
  val r = Runnable {
    runCatching {
      Thread.sleep(6_000)
      EventQueue.invokeAndWait {
        showFrame(frame)
        splashScreen.isVisible = false
        splashScreen.dispose()
      }
    }.onFailure {
      splashScreen.isVisible = false
      splashScreen.dispose()
    }
  }
  Thread(r).start()
}

private fun makeUI(): Component {
  val label = JLabel("Draggable Label")
  val dwl = DragWindowListener()
  label.addMouseListener(dwl)
  label.addMouseMotionListener(dwl)
  label.isOpaque = true
  label.foreground = Color.WHITE
  label.background = Color.BLUE
  label.border = BorderFactory.createEmptyBorder(5, 16 + 5, 5, 2)
  val button = JButton("Exit")
  button.addActionListener { e ->
    // val window = SwingUtilities.getWindowAncestor(e.source as? Component)
    val window = (e.source as? JComponent)?.topLevelAncestor as? Window
    window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  val p = JPanel(BorderLayout())
  p.add(label, BorderLayout.NORTH)
  p.add(box, BorderLayout.SOUTH)
  p.add(JLabel("Alt+Space => System Menu"))
  return p
}

fun createSplashScreen(
  frame: Frame,
  img: Icon,
): JWindow {
  val dwl = DragWindowListener()
  val label = JLabel(img)
  label.addMouseListener(dwl)
  label.addMouseMotionListener(dwl)
  val window = JWindow(frame)
  window.contentPane.add(label)
  window.pack()
  window.setLocationRelativeTo(null)
  return window
}

fun showFrame(frame: JFrame) {
  frame.contentPane.add(makeUI())
  frame.minimumSize = Dimension(100, 100)
  frame.setSize(320, 240)
  frame.setLocationRelativeTo(null)
  frame.isVisible = true
}

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.paint = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.paint = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
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
      isUndecorated = true
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      start(this)
    }
  }
}
