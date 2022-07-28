package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.event.WindowEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicInternalFrameUI

fun makeUI(): Component {
  val closeButton = JButton("close")
  closeButton.addActionListener { e ->
    (SwingUtilities.getRoot(e.source as? Component) as? Window)?.also {
      it.dispatchEvent(WindowEvent(it, WindowEvent.WINDOW_CLOSING))
    }
  }

  val p = JPanel(BorderLayout())
  p.add(JScrollPane(JTree()))
  p.add(closeButton, BorderLayout.SOUTH)

  val internal = makeInternalFrame()
  internal.contentPane.add(p)
  internal.isVisible = true

  return JPanel(BorderLayout()).also {
    it.add(internal)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeInternalFrame(): JInternalFrame {
  val internal = JInternalFrame("Title")
  val ui = internal.ui as? BasicInternalFrameUI ?: return internal
  val title = ui.northPane
  for (l in title.getListeners(MouseMotionListener::class.java)) {
    title.removeMouseMotionListener(l)
  }
  val dwl = DragWindowListener()
  title.addMouseListener(dwl)
  title.addMouseMotionListener(dwl)

  val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
  focusManager.addPropertyChangeListener { e ->
    if ("activeWindow" == e.propertyName) {
      runCatching {
        internal.isSelected = e.newValue != null
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(internal)
      }
    }
  }
  return internal
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      SwingUtilities.getWindowAncestor(e.component)?.also {
        val pt = it.location
        it.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      isUndecorated = true
      minimumSize = Dimension(300, 120)
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      background = Color(0x0, true)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
