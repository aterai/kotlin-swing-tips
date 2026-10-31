package example

import java.awt.*
import java.awt.event.AWTEventListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private val log = JTextArea()

fun createUI(): Component {
  log.isEditable = false
  val toolBar = JToolBar()
  val listIcon = UIManager.getIcon("FileChooser.listViewIcon")
  toolBar.add(createButton(listIcon, createExitClosePopupMenu()))
  toolBar.addSeparator(Dimension(25, 25))
  val detailsIcon = UIManager.getIcon("FileChooser.detailsViewIcon")
  toolBar.add(createButton(detailsIcon, createAutoClosePopupMenu()))
  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createExitClosePopupMenu(): JPopupMenu {
  val popup = JPopupMenu()
  initPopupMenu(popup)
  popup.addMouseListener(object : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      // mouseExited is also fired when the cursor moves onto a child JMenuItem,
      // so wait until the armed state is updated and close only if no item is armed
      EventQueue.invokeLater {
        val isArmed = popup.subElements
          .filterIsInstance<AbstractButton>()
          .any { it.model.isArmed }
        if (!isArmed) {
          popup.isVisible = false
        }
      }
    }
  })
  return popup
}

private fun createAutoClosePopupMenu(): JPopupMenu {
  val popup = AutoClosePopupMenu()
  initPopupMenu(popup)
  return popup
}

private fun createButton(icon: Icon?, popup: JPopupMenu): JButton {
  val button = JButton(icon)
  button.isFocusPainted = false
  button.addActionListener {
    popup.show(button, 0, button.height)
    popup.requestFocusInWindow()
  }
  button.addMouseListener(object : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      button.doClick()
    }
  })
  return button
}

private fun initPopupMenu(popup: JPopupMenu) {
  val group = ButtonGroup()
  createMenuList()
    .map { createMenuItem(it) }
    .forEach { item ->
      item.addActionListener { e ->
        val cmd = e.actionCommand
        log.append("Selected JRadioButton command: %s%n".format(cmd))
      }
      popup.add(item)
      group.add(item)
    }
}

private fun createMenuItem(context: MenuContext): AbstractButton {
  val item = JRadioButtonMenuItem(context.command)
  item.actionCommand = context.command
  item.foreground = context.color
  item.border = BorderFactory.createEmptyBorder()
  return item
}

private fun createMenuList() = listOf(
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
    // The mouse point is converted to this popup's coordinate system,
    // so compare it with the local bounds rather than getBounds(),
    // which depends on the container created by the PopupFactory.
    val r = Rectangle(size)
    // Allow a small vertical gap between the invoker and this popup
    r.grow(0, 5)
    val pt = SwingUtilities.convertPoint(c, e.point, this)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
