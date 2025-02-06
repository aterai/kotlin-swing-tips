package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.geom.Ellipse2D
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createHorizontalBox()
  box.isOpaque = true
  box.background = Color(120, 120, 160)
  box.add(Box.createHorizontalGlue())
  box.border = BorderFactory.createEmptyBorder(60, 10, 60, 10)

  val buttons = listOf(
    RoundButton("005.png", "005d.png", "005g.png"),
    RoundButton("003.png", "003d.png", "003g.png"),
    RoundButton("001.png", "001d.png", "001g.png"),
    RoundButton("002.png", "002d.png", "002g.png"),
    RoundButton("004.png", "004d.png", "004g.png"),
  )
  // TEST: buttons = makeButtonArray2(getClass()) // Set ButtonUI
  buttons.forEach {
    box.add(it)
    box.add(Box.createHorizontalStrut(5))
  }
  box.add(Box.createHorizontalGlue())

  val check = JCheckBox("ButtonBorder Color")
  check.addActionListener { e ->
    val f = (e.source as? JCheckBox)?.isSelected ?: false
    val bgc = if (f) Color.WHITE else Color.BLACK
    buttons.forEach { it.background = bgc }
    box.repaint()
  }

  val p = JPanel()
  p.add(check)

  val alignmentsChoices = JComboBox(ButtonAlignments.entries.toTypedArray())
  alignmentsChoices.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is ButtonAlignments) {
      buttons.forEach { it.alignmentY = item.alignment }
      box.revalidate()
    }
  }
  alignmentsChoices.selectedIndex = 1
  p.add(alignmentsChoices)
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RoundButton(
  i1: String,
  i2: String,
  i3: String,
) : JButton(makeIcon(i1)) {
  private var shape: Shape? = null
  private var base: Shape? = null

  init {
    pressedIcon = makeIcon(i2)
    rolloverIcon = makeIcon(i3)
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    background = Color.BLACK
    isContentAreaFilled = false
    isFocusPainted = false
    alignmentY = Component.TOP_ALIGNMENT
    initShape()
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    val icon = icon
    val i = insets
    val iw = maxOf(icon.iconWidth, icon.iconHeight)
    it.setSize(iw + i.right + i.left, iw + i.top + i.bottom)
  }

  private fun initShape() {
    if (bounds != base) {
      val s = preferredSize ?: Dimension(32, 32)
      base = bounds
      shape = Ellipse2D.Double(0.0, 0.0, s.width - 1.0, s.height - 1.0)
    }
  }

  override fun paintBorder(g: Graphics) {
    initShape()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = background
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(
    x: Int,
    y: Int,
  ): Boolean {
    initShape()
    return shape?.contains(Point(x, y)) ?: super.contains(x, y)
  }
}

private fun makeIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource("example/$path")
  return url?.let { ImageIcon(url) } ?: UIManager.getIcon("OptionPane.errorIcon")
}

enum class ButtonAlignments(
  private val description: String,
  val alignment: Float,
) {
  TOP("Top Alignment", Component.TOP_ALIGNMENT),
  CENTER("Center Alignment", Component.CENTER_ALIGNMENT),
  BOTTOM("Bottom Alignment", Component.BOTTOM_ALIGNMENT),
  ;

  override fun toString() = description
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
