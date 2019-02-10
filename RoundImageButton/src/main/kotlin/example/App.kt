package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.geom.Ellipse2D
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val box = Box.createHorizontalBox()
    box.setOpaque(true)
    box.setBackground(Color(120, 120, 160))
    box.add(Box.createHorizontalGlue())
    box.setBorder(BorderFactory.createEmptyBorder(60, 10, 60, 10))

    val buttons = listOf(
      RoundButton(ImageIcon(javaClass.getResource("005.png")), "005d.png", "005g.png"),
      RoundButton(ImageIcon(javaClass.getResource("003.png")), "003d.png", "003g.png"),
      RoundButton(ImageIcon(javaClass.getResource("001.png")), "001d.png", "001g.png"),
      RoundButton(ImageIcon(javaClass.getResource("002.png")), "002d.png", "002g.png"),
      RoundButton(ImageIcon(javaClass.getResource("004.png")), "004d.png", "004g.png")
    )
    // TEST: buttons = makeButtonArray2(getClass()); // Set ButtonUI
    buttons.forEach {
      box.add(it)
      box.add(Box.createHorizontalStrut(5))
    }
    box.add(Box.createHorizontalGlue())
    add(box, BorderLayout.NORTH)

    val check = JCheckBox("ButtonBorder Color")
    check.addActionListener { e ->
      val bgc = if ((e.getSource() as JCheckBox).isSelected()) Color.WHITE else Color.BLACK
      buttons.forEach { it.setBackground(bgc) }
      box.repaint()
    }

    val p = JPanel()
    p.add(check)

    val alignmentsChoices = JComboBox<ButtonAlignments>(ButtonAlignments.values())
    alignmentsChoices.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        val ba = e.getItem() as ButtonAlignments
        buttons.forEach({ b -> b.setAlignmentY(ba.alingment) })
        box.revalidate()
      }
    }
    alignmentsChoices.setSelectedIndex(1)
    p.add(alignmentsChoices)
    p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    add(p, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class RoundButton : JButton {
  var shape: Shape? = null
  var base: Shape? = null

  constructor() : super()

  constructor(icon: Icon) : super(icon)

  constructor(text: String) : super(text)

  constructor(a: Action) : super(a)

  constructor(text: String, icon: Icon) : super(text, icon)

  constructor(icon: Icon, i2: String, i3: String) : super(icon) {
    setPressedIcon(ImageIcon(javaClass.getResource(i2)))
    setRolloverIcon(ImageIcon(javaClass.getResource(i3)))
  }

  override fun updateUI() {
    super.updateUI()
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    setBackground(Color.BLACK)
    setContentAreaFilled(false)
    setFocusPainted(false)
    setAlignmentY(Component.TOP_ALIGNMENT)
    initShape()
  }

  override fun getPreferredSize(): Dimension {
    val icon = getIcon()
    val i = getInsets()
    val iw = Math.max(icon.getIconWidth(), icon.getIconHeight())
    return Dimension(iw + i.right + i.left, iw + i.top + i.bottom)
  }

  fun initShape() {
    if (getBounds() != base) {
      val s = getPreferredSize()
      base = getBounds()
      shape = Ellipse2D.Double(0.0, 0.0, s.width - 1.0, s.height - 1.0)
    }
  }

  override fun paintBorder(g: Graphics) {
    initShape()
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(getBackground())
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(x: Int, y: Int): Boolean {
    initShape()
    return shape?.contains(x.toDouble(), y.toDouble()) ?: false
  }
}

internal enum class ButtonAlignments private constructor(private val description: String, val alingment: Float) {
  TOP("Top Alignment", Component.TOP_ALIGNMENT),
  CENTER("Center Alignment", Component.CENTER_ALIGNMENT),
  BOTTOM("Bottom Alignment", Component.BOTTOM_ALIGNMENT);

  override fun toString(): String {
    return description
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
