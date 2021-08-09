package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun makeUI(): Component {
  val popup = JPopupMenu()
  popup.add("000")
  popup.add("11111")
  popup.add("2222222")
  popup.add("33333333333333")

  val b = MenuToggleButton("Popup")
  b.setPopupMenu(popup)

  val beep = JButton("Beep")
  beep.addActionListener {
    Toolkit.getDefaultToolkit().beep()
    println("Beep button clicked")
  }

  val model = arrayOf("00000", "111", "2")
  val combo = JComboBox(model)
  combo.isEditable = true

  val key = "PopupMenu.consumeEventOnClose"
  val check = object : JCheckBox(key, UIManager.getBoolean(key)) {
    override fun updateUI() {
      super.updateUI()
      val f = UIManager.getLookAndFeelDefaults().getBoolean(key)
      isSelected = f
      UIManager.put(key, f)
    }
  }
  check.addActionListener { e ->
    UIManager.put(key, (e.source as? JCheckBox)?.isSelected == true)
  }

  val toolBar = JToolBar()
  toolBar.add(b)
  toolBar.add(Box.createGlue())

  val p = JPanel()
  p.add(beep)
  p.add(JComboBox(model))
  p.add(combo)
  p.add(JTextField(16))

  return JPanel(BorderLayout()).also {
    it.componentPopupMenu = popup
    it.add(toolBar, BorderLayout.NORTH)
    it.add(p)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

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

private class MenuToggleButton(text: String? = "", icon: Icon? = null) : JToggleButton() {
  var popup: JPopupMenu? = null
  val handler = object : PopupMenuListener {
    override fun popupMenuCanceled(e: PopupMenuEvent) {
      /* not needed */
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      /* not needed */
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      isSelected = false
    }
  }

  init {
    val action = object : AbstractAction(text) {
      override fun actionPerformed(e: ActionEvent) {
        (e.source as? Component)?.also {
          popup?.show(it, 0, it.height)
        }
      }
    }
    action.putValue(Action.SMALL_ICON, icon)
    setAction(action)
    isFocusable = false
    border = BorderFactory.createEmptyBorder(4, 4, 4, 4 + ARROW_ICON.iconWidth)
  }

  fun setPopupMenu(pop: JPopupMenu) {
    popup = pop
    pop.addPopupMenuListener(handler)
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
