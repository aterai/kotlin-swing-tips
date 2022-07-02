package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.plaf.basic.BasicInternalFrameUI

fun makeUI(): Component {
  val desktop = JDesktopPane()
  desktop.add(createFrame("title #", 1))
  desktop.add(createFrame("title #", 0))

  val idx = AtomicInteger(2)
  val button = JButton("add")
  button.addActionListener {
    desktop.add(createFrame("#", idx.getAndIncrement()))
  }

  val toolBar = JToolBar()
  toolBar.add(button)

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.add(toolBar, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(t: String, i: Int): JInternalFrame {
  val f = JInternalFrame(t + i, true, true, true, true)
  f.setSize(200, 100)
  f.setLocation(5 + 40 * i, 5 + 50 * i)
  val popup = InternalFramePopupMenu()
  f.componentPopupMenu = popup
  // (f.ui as? BasicInternalFrameUI)?.northPane?.componentPopupMenu = popup
  (f.ui as? BasicInternalFrameUI)?.northPane?.inheritsPopupMenu = true
  f.desktopIcon.componentPopupMenu = popup
  EventQueue.invokeLater { f.isVisible = true }
  return f
}

private class InternalFramePopupMenu : JPopupMenu() {
  init {
    val field = object : JTextField(24) {
      private var listener: AncestorListener? = null
      override fun updateUI() {
        removeAncestorListener(listener)
        super.updateUI()
        listener = FocusAncestorListener()
        addAncestorListener(listener)
      }
    }
    val cmd = "Edit Title"
    add(cmd).addActionListener {
      val frame = getInternalFrame(invoker)
      if (frame is JInternalFrame) {
        field.text = frame.title
        val ret = JOptionPane.showConfirmDialog(
          frame.desktopPane,
          field,
          cmd,
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
        )
        if (ret == JOptionPane.OK_OPTION) {
          renameInternalFrameTitle(frame, field.text.trim())
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (getInternalFrame(c) is JInternalFrame) {
      super.show(c, x, y)
    }
  }

  private fun renameInternalFrameTitle(frame: JInternalFrame, title: String) {
    if (title != frame.title) {
      frame.title = title
    }
  }

  private fun getInternalFrame(c: Component) = if (c is JInternalFrame.JDesktopIcon) {
    c.internalFrame
  } else {
    c as? JInternalFrame ?: SwingUtilities.getAncestorOfClass(JInternalFrame::class.java, c)
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    /* not needed */
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    /* not needed */
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
      // UIManager.put("InternalFrame.useTaskBar", Boolean.TRUE)
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
