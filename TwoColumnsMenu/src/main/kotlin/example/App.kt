package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val menu1 = JMenu("File")
  menu1.add("New")
  menu1.add("Open")
  menu1.addSeparator()
  menu1.add("Exit")

  val menu2 = LookAndFeelUtils.createLookAndFeelMenu()
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
  private val renderer = JPanel()

  override fun paintBorder(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    if (c is JComponent) {
      val r = SwingUtilities.calculateInnerArea(c, null)
      val sw = separator.preferredSize.width
      val sh = r.height
      val sx = (r.centerX - sw / 2.0).toInt()
      val sy = r.minY.toInt()
      val g2 = g.create() as? Graphics2D ?: return
      SwingUtilities.paintComponent(g2, separator, renderer, sx, sy, sw, sh)
      g2.dispose()
    }
  }

  override fun getBorderInsets(c: Component?) = insets

  override fun isBorderOpaque() = true
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
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
