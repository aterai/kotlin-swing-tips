package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class MainPanel : JPanel(BorderLayout()) {
  init {
    val pop1 = JPopupMenu().also {
      it.add("000")
      it.add("11111")
      it.addSeparator()
      it.add("2222222")
    }
    val pop2 = JPopupMenu().also {
      it.add("33333333333333")
      it.addSeparator()
      it.add("4444")
      it.add("5555555555")
    }
    val url = javaClass.getResource("ei0021-16.png")
    val rigid = Box.createRigidArea(Dimension(5, 5))

    val toolbar = JToolBar("toolbar").also {
      it.add(makeButton(pop1, "Text", null))
      it.add(rigid)
      it.add(makeButton(pop2, "", ImageIcon(url)))
      it.add(rigid)
      it.add(makeButton(pop2, "Icon+Text", ImageIcon(url)))
      it.add(Box.createGlue())
    }
    add(toolbar, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeButton(popup: JPopupMenu, title: String, icon: ImageIcon?) =
      MenuToggleButton(title, icon).also { it.setPopupMenu(popup) }
}

internal class MenuArrowIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(Color.BLACK)
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
}

internal class MenuToggleButton(text: String = "", icon: Icon? = null) : JToggleButton() {
  protected var popup: JPopupMenu? = null

  constructor(icon: Icon) : this("", icon)

  init {
    val action = object : AbstractAction(text) {
      override fun actionPerformed(e: ActionEvent) {
        val b = e.getSource() as Component
        popup?.show(b, 0, b.getHeight())
      }
    }
    action.putValue(Action.SMALL_ICON, icon)
    setAction(action)
    setFocusable(false)
    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4 + ARROW_ICON.getIconWidth()))
  }

  fun setPopupMenu(pop: JPopupMenu) {
    this.popup = pop
    pop.addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuCanceled(e: PopupMenuEvent) { /* not needed */ }

      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) { /* not needed */ }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        setSelected(false)
      }
    })
  }

  protected override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val dim = getSize()
    val ins = getInsets()
    val x = dim.width - ins.right
    val y = ins.top + (dim.height - ins.top - ins.bottom - ARROW_ICON.getIconHeight()) / 2
    ARROW_ICON.paintIcon(this, g, x, y)
  }

  companion object {
    private val ARROW_ICON = MenuArrowIcon()
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
