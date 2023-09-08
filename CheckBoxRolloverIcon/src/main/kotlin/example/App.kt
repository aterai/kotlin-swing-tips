package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.add(JCheckBox("Default"))
  box.add(Box.createVerticalStrut(5))

  val check1 = object : JCheckBox("JCheckBox.setRolloverIcon(...)") {
    override fun updateUI() {
      super.updateUI()
      val icon = CheckBoxRolloverIcon()
      // setIcon(icon)
      setPressedIcon(icon)
      setSelectedIcon(icon)
      setRolloverIcon(icon)
    }
  }
  // check1.setRolloverIcon(new CheckBoxRolloverIcon());
  box.add(check1)
  box.add(Box.createVerticalStrut(5))

  val check2 = object : JCheckBox("UIManager CheckBox.icon") {
    override fun updateUI() {
      super.updateUI()
      setIcon(CheckBoxIcon())
    }
  }
  box.add(check2)
  box.add(Box.createVerticalStrut(5))

  val check3 = object : JCheckBox("UIDefaults CheckBox[MouseOver].iconPainter") {
    override fun updateUI() {
      super.updateUI()
      val d = UIManager.getLookAndFeelDefaults()
      val painter0 = getIconPainter(d, "Focused+Selected")
      val painter1 = getIconPainter(d, "MouseOver")
      if (painter0 == null || painter1 == null) {
        return
      }
      val painter2 = Painter { g: Graphics2D, cb: JCheckBox, width: Int, height: Int ->
        painter1.paint(g, cb, width, height)
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = Color.WHITE
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f)
        cb.setSelected(true)
        painter0.paint(g2, cb, width, height)
        cb.setSelected(false)
        g2.dispose()
      }
      d["CheckBox[MouseOver].iconPainter"] = painter2
      d["CheckBox[Focused+MouseOver].iconPainter"] = painter2
      putClientProperty("Nimbus.Overrides", d)
      putClientProperty("Nimbus.Overrides.InheritDefaults", true)
    }
  }
  box.add(check3)
  box.add(Box.createVerticalStrut(5))

  return JPanel().also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.add(box, BorderLayout.NORTH)
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getIconPainter(d: UIDefaults, status: String): Painter<JCheckBox>? {
  val key = "CheckBox[%s].iconPainter".format(status)
  val painter = d[key]
  return painter as? Painter<JCheckBox>
}

private class CheckBoxIcon : Icon {
  private val checkIcon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (c is AbstractButton) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(x, y)
      val model = c.model
      if (!model.isSelected && model.isRollover) {
        checkIcon.paintIcon(c, g2, 0, 0)
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f)
        c.setSelected(true)
        checkIcon.paintIcon(c, g2, 0, 0)
        c.setSelected(false)
      } else {
        checkIcon.paintIcon(c, g2, 0, 0)
      }
      g2.dispose()
    }
  }

  override fun getIconWidth() = checkIcon.iconWidth

  override fun getIconHeight() = checkIcon.iconHeight
}

private class CheckBoxRolloverIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (c is AbstractButton) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(x, y)
      g2.color = Color(255, 155, 155, 100)
      g2.fillRect(2, 2, iconWidth - 4, iconHeight - 4)
      g2.color = Color.RED
      g2.drawLine(9, 3, 9, 3)
      g2.drawLine(8, 4, 9, 4)
      g2.drawLine(7, 5, 9, 5)
      g2.drawLine(6, 6, 8, 6)
      g2.drawLine(3, 7, 7, 7)
      g2.drawLine(4, 8, 6, 8)
      g2.drawLine(5, 9, 5, 9)
      g2.drawLine(3, 5, 3, 5)
      g2.drawLine(3, 6, 4, 6)
      g2.dispose()
    }
  }

  override fun getIconWidth() = 18

  override fun getIconHeight() = 18
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
