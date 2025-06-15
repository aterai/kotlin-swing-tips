package example

import java.awt.*
import java.awt.event.AWTEventListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private val log = JTextArea()

fun makeUI(): Component {
  log.isEditable = false
  val toolBar = JToolBar()
  toolBar.add(makeButton0())
  toolBar.addSeparator(Dimension(25, 25))
  toolBar.add(makeButton1())
  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton0(): JButton {
  val popup = JPopupMenu()
  initPopupMenu(popup)
  popup.addMouseListener(object : MouseAdapter() {
    override fun mouseExited(e: MouseEvent?) {
      EventQueue.invokeLater {
        var b = false
        for (me in popup.getSubElements()) {
          if (me is AbstractButton) {
            b = b or ((me as? AbstractButton)?.getModel()?.isArmed == true)
          }
        }
        if (!b) {
          popup.setVisible(false)
        }
      }
    }
  })
  val button = JButton(UIManager.getIcon("FileChooser.listViewIcon"))
  button.setFocusPainted(false)
  button.addActionListener { e ->
    popup.show(button, 0, button.getHeight())
    popup.requestFocusInWindow()
  }
  button.addMouseListener(object : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      (e.component as? AbstractButton)?.doClick()
    }
  })
  return button
}

private fun makeButton1(): JButton {
  val popup: JPopupMenu = AutoClosePopupMenu()
  initPopupMenu(popup)
  val button = JButton(UIManager.getIcon("FileChooser.detailsViewIcon"))
  button.setFocusPainted(false)
  button.addActionListener { e ->
    popup.show(button, 0, button.getHeight())
    popup.requestFocusInWindow()
  }
  button.addMouseListener(object : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      (e.component as? AbstractButton)?.doClick()
    }
  })
  return button
}

private fun initPopupMenu(popup: JPopupMenu) {
  val bg = ButtonGroup()
  makeMenuList()
    .toList()
    .map { makeMenuButton(it) }
    .forEach {
      it.addActionListener { e ->
        val cmd = e.getActionCommand()
        log.append("Selected JRadioButton command: %s%n".format(cmd))
      }
      popup.add(it)
      bg.add(it)
    }
}

private fun makeMenuButton(m: MenuContext): AbstractButton {
  val b = JRadioButtonMenuItem(m.command)
  b.actionCommand = m.command
  b.foreground = m.color
  b.border = BorderFactory.createEmptyBorder()
  return b
}

private fun makeMenuList() = listOf(
  MenuContext("BLUE", Color.BLUE),
  MenuContext("CYAN", Color.CYAN),
  MenuContext("GREEN", Color.GREEN),
  MenuContext("MAGENTA", Color.MAGENTA),
  MenuContext("ORANGE", Color.ORANGE),
  MenuContext("PINK", Color.PINK),
  MenuContext("RED", Color.RED),
  MenuContext("YELLOW", Color.YELLOW),
)

private class AutoClosePopupMenu : JPopupMenu() {
  private var listener: PopupMenuListener? = null

  override fun updateUI() {
    removePopupMenuListener(listener)
    super.updateUI()
    listener = AwtPopupMenuListener()
    addPopupMenuListener(listener)
  }

  private fun checkAutoClose(e: MouseEvent) {
    val c = e.component
    val r = bounds
    r.grow(0, 5)
    val pt = SwingUtilities.convertPoint(c, e.getPoint(), this)
    if (!r.contains(pt) && c !is JButton) {
      setVisible(false)
    }
  }

  private inner class AwtPopupMenuListener : PopupMenuListener {
    private val a = AWTEventListener { e ->
      if (e is MouseEvent) {
        val id = e.getID()
        if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_EXITED) {
          checkAutoClose(e)
        }
      }
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
      val mask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
      Toolkit.getDefaultToolkit().addAWTEventListener(a, mask)
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(a)
    }

    override fun popupMenuCanceled(e: PopupMenuEvent?) {
      // not needed
    }
  }
}

private data class MenuContext(
  val command: String,
  val color: Color,
)

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
