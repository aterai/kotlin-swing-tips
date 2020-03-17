package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.event.WindowEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicInternalFrameUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val closeButton = JButton("close")
    closeButton.addActionListener { e ->
      val c = e.getSource() as? Component ?: return@addActionListener
      (SwingUtilities.getRoot(c) as? Window)?.also {
        it.dispatchEvent(WindowEvent(it, WindowEvent.WINDOW_CLOSING))
      }
    }

    val p = JPanel(BorderLayout())
    p.add(JScrollPane(JTree()))
    p.add(closeButton, BorderLayout.SOUTH)

    val internal = makeInternalFrame()
    internal.getContentPane().add(p)
    internal.setVisible(true)

    add(internal)
    setOpaque(false)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeInternalFrame(): JInternalFrame {
    val internal = JInternalFrame("Title")
    val ui = internal.getUI() as? BasicInternalFrameUI ?: return internal
    val title = ui.getNorthPane()
    for (l in title.getListeners(MouseMotionListener::class.java)) {
      title.removeMouseMotionListener(l)
    }
    val dwl = DragWindowListener()
    title.addMouseListener(dwl)
    title.addMouseMotionListener(dwl)

    val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    focusManager.addPropertyChangeListener { e ->
      if ("activeWindow" == e.getPropertyName()) {
        runCatching {
          internal.setSelected(e.getNewValue() != null)
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(internal)
        }
      }
    }
    return internal
  }
}

internal class DragWindowListener : MouseAdapter() {
  private val startPt = Point()

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.setLocation(e.getPoint())
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      (SwingUtilities.getRoot(e.getComponent()) as? Window)?.also {
        val pt = it.getLocation()
        it.setLocation(pt.x - startPt.x + e.getX(), pt.y - startPt.y + e.getY())
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
      setUndecorated(true)
      setMinimumSize(Dimension(300, 120))
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(MainPanel())
      setBackground(Color(0x0, true))
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
