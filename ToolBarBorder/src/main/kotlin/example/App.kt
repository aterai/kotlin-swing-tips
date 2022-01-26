package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalBorders

fun makeUI(): Component {
  val toolBar1 = object : JToolBar("Customized ToolBarBorder") {
    override fun updateUI() {
      super.updateUI()
      border = ToolBarDragBorder()
    }
  }.also {
    it.add(JLabel("<- Customized Border"))
    it.addSeparator()
    it.add(JRadioButton("JRadioButton"))
    it.add(JToggleButton("JToggleButton"))
  }

  val toolBar2 = JToolBar("default").also {
    it.add(JLabel("<- Default Border"))
    it.addSeparator()
    it.add(JCheckBox("JCheckBox"))
    it.add(JButton("JButton"))
  }

  return JPanel(BorderLayout()).also {
    it.add(toolBar1, BorderLayout.NORTH)
    it.add(toolBar2, BorderLayout.SOUTH)
    it.add(JScrollPane(JTextArea()))
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private class ToolBarDragBorder : MetalBorders.ToolBarBorder() {
  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
    val tb = c as? JToolBar ?: return
    if (tb.isFloatable) {
      if (tb.orientation == HORIZONTAL) {
        val cy = (h - DRAG_ICON.iconHeight) / 2
        DRAG_ICON.paintIcon(c, g, x, y + cy)
      } else { // vertical
        super.paintBorder(c, g, x, y, w, h)
      }
    }
  }

  companion object {
    private val DRAG_ICON = ToolBarDragIcon()
  }
}

private class ToolBarDragIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.GRAY
    val x2 = iconWidth / 2 - 1
    val y2 = iconHeight / 2 - 1
    g2.fillRect(x2, y2 - 6, 2, 2)
    g2.fillRect(x2, y2 - 2, 2, 2)
    g2.fillRect(x2, y2 + 2, 2, 2)
    g2.fillRect(x2, y2 + 6, 2, 2)
    g2.dispose()
  }

  override fun getIconWidth() = 14

  override fun getIconHeight() = 16
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
