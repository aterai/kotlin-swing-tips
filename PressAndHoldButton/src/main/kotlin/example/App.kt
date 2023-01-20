package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea("press and hold the button for 1000 milliseconds\n")
  log.isEditable = false

  val popupMenu = JPopupMenu()
  popupMenu.layout = GridLayout(0, 3, 5, 5)
  val bg = ButtonGroup()
  makeMenuList()
    .map { makeMenuButton(it) }
    .forEach {
      it.addActionListener {
        val cmd = bg.selection?.actionCommand ?: "null"
        log.append("Selected JRadioButton command: $cmd\n")
        popupMenu.isVisible = false
      }
      popupMenu.add(it)
      bg.add(it)
    }

  val icon = UIManager.getIcon("FileChooser.detailsViewIcon")
  val button = PressAndHoldButton(icon, popupMenu)
  button.addActionListener {
    val cmd = bg.selection?.actionCommand ?: "null"
    log.append("Selected action command: $cmd\n")
  }

  val toolBar = JToolBar()
  toolBar.add(button)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenuList() = listOf(
  MenuContext("BLACK", Color.BLACK),
  MenuContext("BLUE", Color.BLUE),
  MenuContext("CYAN", Color.CYAN),
  MenuContext("GREEN", Color.GREEN),
  MenuContext("MAGENTA", Color.MAGENTA),
  MenuContext("ORANGE", Color.ORANGE),
  MenuContext("PINK", Color.PINK),
  MenuContext("RED", Color.RED),
  MenuContext("YELLOW", Color.YELLOW)
)

private fun makeMenuButton(m: MenuContext): AbstractButton {
  val b = JRadioButton(m.command)
  b.actionCommand = m.command
  b.foreground = m.color
  b.border = BorderFactory.createEmptyBorder()
  return b
}

private class PressAndHoldButton(icon: Icon?, popupMenu: JPopupMenu?) : JButton(icon) {
  private var handler: PressAndHoldHandler? = null
  private val popupMenu: JPopupMenu?

  init {
    action.putValue(Action.SMALL_ICON, icon)
    this.popupMenu = popupMenu
  }

  override fun updateUI() {
    removeMouseListener(handler)
    super.updateUI()
    if (popupMenu != null) {
      SwingUtilities.updateComponentTreeUI(popupMenu)
    }
    handler = PressAndHoldHandler()
    action = handler
    addMouseListener(handler)
    isFocusable = false
    border = BorderFactory.createEmptyBorder(4, 4, 4, 4 + Companion.ARROW_ICON.iconWidth)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val r = SwingUtilities.calculateInnerArea(this, null)
    val cy = (r.height - Companion.ARROW_ICON.iconHeight) / 2
    Companion.ARROW_ICON.paintIcon(this, g, r.x + r.width, r.y + cy)
  }

  private inner class PressAndHoldHandler : AbstractAction(), MouseListener {
    private val holdTimer = Timer(1000) { e ->
      val timer = e.source as Timer
      if (popupMenu != null && getModel().isPressed && timer.isRunning) {
        timer.stop()
        popupMenu.show(this@PressAndHoldButton, 0, height)
        popupMenu.requestFocusInWindow()
      }
    }

    init {
      holdTimer.initialDelay = 1000
    }

    override fun actionPerformed(e: ActionEvent) {
      if (holdTimer.isRunning) {
        holdTimer.stop()
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val c = e.component
      if (SwingUtilities.isLeftMouseButton(e) && c.isEnabled) {
        holdTimer.start()
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      holdTimer.stop()
    }

    override fun mouseExited(e: MouseEvent) {
      if (holdTimer.isRunning) {
        holdTimer.stop()
      }
    }

    override fun mouseEntered(e: MouseEvent) {
      /* not needed */
    }

    override fun mouseClicked(e: MouseEvent) {
      /* not needed */
    }
  }

  companion object {
    private val ARROW_ICON = MenuArrowIcon()
  }
}

private data class MenuContext(val command: String, val color: Color)

private class MenuArrowIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.BLACK
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
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
