package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.geom.Ellipse2D
import javax.swing.*

fun createUI(): Component {
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
  // TEST: buttons = createButtonArray2(getClass()) // Set ButtonUI
  buttons.forEach {
    box.add(it)
    box.add(Box.createHorizontalStrut(5))
  }
  box.add(Box.createHorizontalGlue())

  val check = JCheckBox("ButtonBorder Color")
  check.addActionListener { e ->
    val f = (e.source as? JCheckBox)?.isSelected ?: false
    val color = if (f) Color.WHITE else Color.BLACK
    buttons.forEach { it.background = color }
    box.repaint()
  }

  val p = JPanel()
  p.add(check)

  val alignmentChoices = JComboBox(ButtonAlignment.entries.toTypedArray())
  alignmentChoices.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is ButtonAlignment) {
      buttons.forEach { it.alignmentY = item.alignment }
      box.revalidate()
    }
  }
  alignmentChoices.selectedIndex = 1
  p.add(alignmentChoices)
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RoundButton(
  iconPath: String,
  pressedIconPath: String,
  rolloverIconPath: String,
) : JButton(createIcon(iconPath)) {
  // lateinit has no initializer, so the shape created in updateUI()
  // (called from the JButton constructor) is not reset afterwards
  private lateinit var shape: Shape
  private var baseBounds: Rectangle? = null

  init {
    pressedIcon = createIcon(pressedIconPath)
    rolloverIcon = createIcon(rolloverIconPath)
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
    // Use a square of the longer icon side so that the border forms a circle
    val size = maxOf(icon.iconWidth, icon.iconHeight)
    it.setSize(size + i.right + i.left, size + i.top + i.bottom)
  }

  private fun initShape() {
    if (bounds != baseBounds) {
      val s = preferredSize ?: Dimension(32, 32)
      baseBounds = bounds
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
    // The shape is always created in updateUI() called from the constructor,
    // so only the inside of the circle responds to the mouse
    return shape.contains(x.toDouble(), y.toDouble())
  }
}

private fun createIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource("example/$path")
  return url?.let { ImageIcon(url) } ?: UIManager.getIcon("OptionPane.errorIcon")
}

enum class ButtonAlignment(
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
