package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.plaf.basic.ComboPopup
import kotlin.math.max

fun makeUI(): Component {
  val model = makeModel()
  val combo = object : JComboBox<String>(model) {
    override fun getPreferredSize(): Dimension {
      val i = getInsets()
      val d = super.getPreferredSize()
      val w = max(100.0, d.width.toDouble()).toInt()
      val h = d.height
      val buttonWidth = 20 // ???
      return Dimension(buttonWidth + w + i.left + i.right, h + i.top + i.bottom)
    }

    override fun updateUI() {
      super.updateUI()
      setModel(getModel())
    }

    override fun setModel(model: ComboBoxModel<String>) {
      super.setModel(model)
      val rowCount = (model.size + 1) / 2
      setMaximumRowCount(rowCount)
      EventQueue.invokeLater {
        val o = getAccessibleContext().getAccessibleChild(0)
        if (o is ComboPopup) {
          val list = o.list
          list.setLayoutOrientation(JList.VERTICAL_WRAP)
          list.setVisibleRowCount(rowCount)
          val b0 = list.border
          val b1 = ColumnRulesBorder()
          list.setBorder(BorderFactory.createCompoundBorder(b0, b1))
          list.setFixedCellWidth((getPreferredSize().width - 2) / 2)
        }
      }
    }
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel().also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JComboBox(makeModel()))
    it.add(combo)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("111")
  it.addElement("2222")
  it.addElement("3")
  it.addElement("44444")
  it.addElement("55555")
  it.addElement("66")
  it.addElement("777")
  it.addElement("8")
  it.addElement("9999")
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
