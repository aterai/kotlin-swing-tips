package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

fun makeUI() = JPanel(BorderLayout()).also {
  val cl = Thread.currentThread().contextClassLoader
  val toolbar = JToolBar("toolbar")
  toolbar.add(PressAndHoldButton(ImageIcon(cl.getResource("example/ei0021-16.png"))))
  it.add(toolbar, BorderLayout.NORTH)
  it.add(JLabel("press and hold the button for 1000 milliseconds"))
  it.preferredSize = Dimension(320, 240)
}

private class PressAndHoldButton(icon: Icon?) : JButton(icon) {
  private var handler: PressAndHoldHandler? = null

  init {
    action.putValue(Action.SMALL_ICON, icon)
  }

  override fun updateUI() {
    removeMouseListener(handler)
    super.updateUI()
    handler = PressAndHoldHandler().also {
      SwingUtilities.updateComponentTreeUI(it.pop)
      action = it
      addMouseListener(it)
    }
    isFocusable = false
    border = BorderFactory.createEmptyBorder(4, 4, 4, 4 + ARROW_ICON.iconWidth)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val r = SwingUtilities.calculateInnerArea(this, null)
    ARROW_ICON.paintIcon(this, g, r.x + r.width, r.y + (r.height - ARROW_ICON.iconHeight) / 2)
  }

  companion object {
    private val ARROW_ICON = MenuArrowIcon()
  }
}

private class PressAndHoldHandler : AbstractAction(), MouseListener {
  val pop = JPopupMenu()
  private val bg = ButtonGroup()
  private var arrowButton: AbstractButton? = null
  private val holdTimer = Timer(1000) { e ->
    println("InitialDelay(1000)")
    arrowButton?.also {
      val timer = e.source as? Timer
      if (it.model.isPressed && timer?.isRunning == true) {
        timer.stop()
        pop.show(it, 0, it.height)
        pop.requestFocusInWindow()
      }
    }
  }

  init {
    holdTimer.initialDelay = 1000
    pop.layout = GridLayout(0, 3, 5, 5)
    makeMenuList()
      .map { makeMenuButton(it) }
      .forEach {
        it.addActionListener {
          println(bg.selection.actionCommand)
          pop.isVisible = false
        }
        pop.add(it)
        bg.add(it)
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

  override fun actionPerformed(e: ActionEvent) {
    println("actionPerformed")
    if (holdTimer.isRunning) {
      val model = bg.selection
      if (model != null) {
        println(model.actionCommand)
      }
      holdTimer.stop()
    }
  }

  override fun mousePressed(e: MouseEvent) {
    println("mousePressed")
    val c = e.component
    if (SwingUtilities.isLeftMouseButton(e) && c is AbstractButton && c.isEnabled) {
      arrowButton = c
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

  companion object {
    private fun makeMenuButton(m: MenuContext): AbstractButton {
      val b = JRadioButton(m.command)
      b.actionCommand = m.command
      b.foreground = m.color
      b.border = BorderFactory.createEmptyBorder()
      return b
    }
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
