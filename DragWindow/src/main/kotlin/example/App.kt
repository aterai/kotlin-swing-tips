package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun start(frame: JFrame) {
  val cl = Thread.currentThread().contextClassLoader
  val img = ImageIcon(cl.getResource("example/splash.png"))
  val splashScreen = createSplashScreen(frame, img)
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
    val c = e.source as? JComponent
    val frame = c?.topLevelAncestor as? JFrame
    frame?.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
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

fun createSplashScreen(frame: JFrame?, img: ImageIcon?): JWindow {
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
