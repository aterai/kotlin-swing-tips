package example

import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun makeUI(): Component {
  val model = arrayOf("111", "2222", "33333")
  val combo1 = object : JComboBox<String>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      setRenderer(RoundedCornerListCellRenderer())
    }
  }

  val combo2 = object : JComboBox<String>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      setRenderer(RoundedCornerListCellRenderer())
      setEditable(true)
    }
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(makeTitledPanel("Default JComboBox", JComboBox(model)))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("RoundedCornerListCellRenderer", combo1))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("RoundedCornerListCellRenderer(editable)", combo2))

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class RoundedCornerListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer = object : DefaultListCellRenderer() {
    override fun paintComponent(g: Graphics) {
      if (icon != null) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.paint = background
        val r = SwingUtilities.calculateInnerArea(this, null)
        val rr = RoundRectangle2D.Float(
          r.x.toFloat(),
          r.y.toFloat(),
          r.width.toFloat(),
          r.height.toFloat(),
          10f,
          10f,
        )
        g2.fill(rr)
        super.paintComponent(g2)
        g2.dispose()
      } else {
        super.paintComponent(g)
      }
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JLabel) {
      val label = c
      label.isOpaque = false
      label.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      label.iconTextGap = 0
      label.icon = if (index >= 0) GapIcon() else null
      label.foreground = if (isSelected) {
        Color(list.selectionForeground.rgb) // Nimbus DerivedColor bug?
      } else {
        list.foreground
      }
    }
    return c
  }
}

private class GapIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Empty paint
  }

  override fun getIconWidth() = 2

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
