package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val toolbar = JToolBar("toolbar")
    toolbar.add(PressAndHoldButton(ImageIcon(javaClass.getResource("ei0021-16.png"))))
    add(toolbar, BorderLayout.NORTH)
    add(JLabel("press and hold the button for 1000 milliseconds"))
    preferredSize = Dimension(320, 240)
  }
}

class PressAndHoldButton(icon: Icon?) : JButton(icon) {
  private var handler: PressAndHoldHandler? = null
  override fun updateUI() {
    removeMouseListener(handler)
    super.updateUI()
    handler = PressAndHoldHandler()
    SwingUtilities.updateComponentTreeUI(handler!!.pop)
    action = handler
    addMouseListener(handler)
    isFocusable = false
    border = BorderFactory.createEmptyBorder(4, 4, 4, 4 + ARROW_ICON.iconWidth)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    // val dim = size
    // val ins = insets
    val r = SwingUtilities.calculateInnerArea(this, null)
    val x = r.x // dim.width - ins.right
    val y = r.y + (r.height - ARROW_ICON.iconHeight) / 2
    //   ins.top + (dim.height - ins.top - ins.bottom - ARROW_ICON.iconHeight) / 2
    ARROW_ICON.paintIcon(this, g, x, y)
  }

  companion object {
    private val ARROW_ICON: Icon = MenuArrowIcon()
  }

  init {
    action.putValue(Action.SMALL_ICON, icon)
  }
}

class PressAndHoldHandler : AbstractAction(), MouseListener {
  val pop = JPopupMenu()
  private val bg = ButtonGroup()
  private var arrowButton: AbstractButton? = null
  private val holdTimer = Timer(1000, null)
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

  init {
    holdTimer.initialDelay = 1000
    holdTimer.addActionListener {
      println("InitialDelay(1000)")
      arrowButton?.also {
        if (it.model.isPressed && holdTimer.isRunning) {
          holdTimer.stop()
          pop.show(it, 0, it.height)
          pop.requestFocusInWindow()
        }
      }
    }
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
}

data class MenuContext(val command: String, val color: Color)

class MenuArrowIcon : Icon {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
