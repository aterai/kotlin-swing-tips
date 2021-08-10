package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

fun makeUI(): Component {
  val menu1 = JMenu("File")
  menu1.add("New")
  menu1.add("Open")
  menu1.addSeparator()
  menu1.add("Exit")

  val menu2 = LookAndFeelUtil.createLookAndFeelMenu()
  val pop = menu2.popupMenu
  pop.layout = GridLayout(0, 2, 8, 0)
  pop.border = BorderFactory.createCompoundBorder(pop.border, ColumnRulesBorder())

  val mb = JMenuBar()
  mb.add(menu1)
  mb.add(menu2)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColumnRulesBorder : Border {
  private val insets = Insets(0, 0, 0, 0)
  private val separator = JSeparator(SwingConstants.VERTICAL)
  override fun paintBorder(c: Component?, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    if (c is JComponent) {
      val r = SwingUtilities.calculateInnerArea(c, null)
      val sw = separator.preferredSize.width
      val sh = r.height
      val sx = (r.centerX - sw / 2.0).toInt()
      val sy = r.minY.toInt()
      val g2 = g.create() as Graphics2D
      SwingUtilities.paintComponent(g2, separator, c, sx, sy, sw, sh)
      g2.dispose()
    }
  }

  override fun getBorderInsets(c: Component?) = insets

  override fun isBorderOpaque() = true
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
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
