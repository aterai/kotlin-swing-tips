package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.AbstractBorder

fun makeUI(): Component {
  val field1 = JTextField(20)
  field1.text = "1111111111111111"

  val field2 = object : JTextField(20) {
    private var handler: FocusListener? = null

    override fun updateUI() {
      removeFocusListener(handler)
      super.updateUI()
      setOpaque(false)
      setBorder(RoundedCornerBorder())
      handler = FocusBorderListener()
      addFocusListener(handler)
    }

    override fun paintComponent(g: Graphics) {
      val b = border
      if (!isOpaque && b is RoundedCornerBorder) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = getBackground()
        g2.fill(b.getBorderShape(0.0, 0.0, width - 1.0, height - 1.0))
        g2.dispose()
      }
      super.paintComponent(g)
    }
  }
  field2.text = "2222222222222"

  val check = JCheckBox("setEnabled", true)
  check.addActionListener {
    val b = check.isSelected
    field1.setEnabled(b)
    field2.setEnabled(b)
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)

  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.add(makeTitledPanel("Default:", field1))
  p.add(makeTitledPanel("setBorder(new RoundedCornerBorder())", field2))
  p.add(box)

  val mb = JMenuBar()
  mb.add(example.LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.add(p, BorderLayout.NORTH)
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.setBorder(BorderFactory.createTitledBorder(title))
  p.setOpaque(false)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

private class RoundedCornerBorder : AbstractBorder() {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val dx = x.toDouble()
    val dy = y.toDouble()
    val dw = width.toDouble()
    val dh = height.toDouble()
    val border = getBorderShape(dx, dy, dw - 1.0, dh - 1.0)
    g2.paint = ALPHA_ZERO
    val corner = Area(Rectangle2D.Double(dx, dy, dw, dh))
    corner.subtract(Area(border))
    g2.fill(corner)
    g2.paint = if (c.hasFocus()) {
      Color(0x4F_C1_E9)
    } else if (c.isEnabled) {
      Color.LIGHT_GRAY
    } else {
      Color.WHITE
    }
    g2.draw(border)
    g2.dispose()
  }

  fun getBorderShape(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
  ) = RoundRectangle2D.Double(
    x,
    y,
    w,
    h,
    ARC.toDouble(),
    ARC.toDouble(),
  )

  override fun getBorderInsets(c: Component) = Insets(ARC, ARC, ARC, ARC)

  override fun getBorderInsets(
    c: Component,
    insets: Insets,
  ): Insets {
    insets[ARC, ARC, ARC] = ARC
    return insets
  }

  companion object {
    private val ALPHA_ZERO = Color(0x0, true)
    private const val ARC = 4
  }
}

private class FocusBorderListener : FocusListener {
  override fun focusGained(e: FocusEvent) {
    update(e.component)
  }

  override fun focusLost(e: FocusEvent) {
    update(e.component)
  }

  private fun update(c: Component) {
    c.repaint()
  }
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
