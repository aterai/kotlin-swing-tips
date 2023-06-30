package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun makeUI(): Component {
  val button = MenuToggleButton("JToggleButton")
  button.setPopupMenu(makePopup())

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(4))
  box.add(JLabel("JFrame Footer"))
  box.add(Box.createHorizontalStrut(16))

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(JTree()))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopup(): JPopupMenu {
  val popup = JPopupMenu()
  popup.add(makePopupHeader())
  popup.add("JMenuItem")
  popup.addSeparator()
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  val menu = JMenu("JMenu")
  menu.add("Sub JMenuItem 1")
  menu.add("Sub JMenuItem 2")
  popup.add(menu)
  return popup
}

private fun makePopupHeader(): JLabel {
  val header = object : JLabel("Header", CENTER) {
    private var listener: MouseAdapter? = null

    override fun updateUI() {
      removeMouseListener(listener)
      removeMouseMotionListener(listener)
      super.updateUI()
      listener = example.PopupHeaderMouseListener()
      addMouseListener(listener)
      addMouseMotionListener(listener)
    }

    override fun getMaximumSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = Short.MAX_VALUE.toInt()
      return d
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 24
      return d
    }
  }
  header.isOpaque = true
  header.background = Color.LIGHT_GRAY
  return header
}

private class PopupHeaderMouseListener : MouseAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.getPoint()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.getComponent()
    val w = SwingUtilities.getWindowAncestor(c)
    if (w != null && SwingUtilities.isLeftMouseButton(e)) {
      if (w.type == Window.Type.POPUP) { // Popup$HeavyWeightWindow
        val pt = e.getLocationOnScreen()
        w.setLocation(pt.x - startPt.x, pt.y - startPt.y)
      } else { // Popup$LightWeightWindow
        val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, c)
        val pt = popup.location
        popup.setLocation(pt.x - startPt.x + e.getX(), pt.y - startPt.y + e.getY())
      }
    }
  }
}

private class MenuArrowIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.BLACK
    g2.drawLine(2, 5, 6, 5)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 3, 4, 3)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
}

private class MenuToggleButton(text: String = "", icon: Icon? = null) : JToggleButton() {
  private var popup: JPopupMenu? = null

  init {
    val action = object : AbstractAction(text) {
      override fun actionPerformed(e: ActionEvent) {
        popup?.also {
          it.show(e.source as? Component, 0, -it.preferredSize.height)
        }
      }
    }
    action.putValue(Action.SMALL_ICON, icon)
    setAction(action)
    isFocusable = false
    border = BorderFactory.createEmptyBorder(4, 4, 4, 4 + ARROW_ICON.iconWidth)
  }

  fun setPopupMenu(pop: JPopupMenu) {
    this.popup = pop
    val pml = object : PopupMenuListener {
      override fun popupMenuCanceled(e: PopupMenuEvent) {
        // not needed
      }

      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        // not needed
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        isSelected = false
      }
    }
    pop.addPopupMenuListener(pml)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val dim = size
    val ins = insets
    val x = dim.width - ins.right
    val y = ins.top + (dim.height - ins.top - ins.bottom - ARROW_ICON.iconHeight) / 2
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
