package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.roundToInt

private const val TXT = "***********************"
private const val LINE_WIDTH = 1
private const val BI_GAP = 2

private fun makeButton(title: String, color: Color, first: Boolean): AbstractButton {
  // https://java-swing-tips.blogspot.com/2008/11/rounded-corner-jbutton.html
  val b = object : JToggleButton(title) {
    @Transient
    private val icon = ArrowToggleButtonBarCellIcon()
    override fun contains(x: Int, y: Int) =
      icon.shape?.contains(x.toDouble(), y.toDouble()) == true

    override fun getPreferredSize() = Dimension(icon.iconWidth, icon.iconHeight)

    override fun paintComponent(g: Graphics) {
      icon.paintIcon(this, g, 0, 0)
      super.paintComponent(g)
    }
  }
  b.icon = object : Icon {
    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
      g.color = Color.GRAY
      g.drawOval(x, y, iconWidth, iconHeight)
    }

    override fun getIconWidth() = 12

    override fun getIconHeight() = 12
  }
  b.isContentAreaFilled = false
  val th = ArrowToggleButtonBarCellIcon.TH
  val left = (if (first) 0 else th) + LINE_WIDTH + BI_GAP
  b.border = BorderFactory.createEmptyBorder(0, left, 0, th)
  b.horizontalAlignment = SwingConstants.LEFT
  b.isFocusPainted = false
  b.isOpaque = false
  b.background = color
  return b
}

private fun makeContainer(overlap: Int): Container {
  // https://java-swing-tips.blogspot.com/2013/12/breadcrumb-navigation-with-jradiobutton.html
  val p = object : JPanel(FlowLayout(FlowLayout.LEADING, -overlap, 0)) {
    override fun isOptimizedDrawingEnabled() = false
  }
  p.border = BorderFactory.createEmptyBorder(0, overlap, 0, 0)
  p.isOpaque = false
  return p
}

private fun makeBreadcrumbList(
  overlap: Int,
  color: Color,
  list: List<String>
): Component {
  val p = makeContainer(overlap + LINE_WIDTH)
  val bg = ButtonGroup()
  var f = true
  for (title in list) {
    val b = makeButton(title, color, f)
    p.add(b)
    bg.add(b)
    f = false
  }
  return p
}

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1))
  p.border = BorderFactory.createEmptyBorder(20, 10, 20, 0)
  p.add(makeBreadcrumbList(0, Color.PINK, listOf("overlap1:", "0px", TXT)))
  p.add(makeBreadcrumbList(5, Color.CYAN, listOf("overlap2:", "5px", TXT)))
  p.add(makeBreadcrumbList(9, Color.ORANGE, listOf("overlap3:", "9px", TXT)))
  p.preferredSize = Dimension(320, 240)
  return p
}

// https://ateraimemo.com/Swing/ToggleButtonBar.html
// https://java-swing-tips.blogspot.com/2012/11/make-togglebuttonbar-with-jradiobuttons.html
private class ArrowToggleButtonBarCellIcon : Icon {
  var shape: Shape? = null
    private set

  fun makeShape(parent: Container, c: Component, x: Int, y: Int): Shape {
    val w = c.width - 1
    val h = c.height - 1
    val h2 = (h * .5).roundToInt().toDouble()
    val w2 = TH.toDouble()
    val p: Path2D = Path2D.Double()
    p.moveTo(0.0, 0.0)
    p.lineTo(w - w2, 0.0)
    p.lineTo(w.toDouble(), h2)
    p.lineTo(w - w2, h.toDouble())
    p.lineTo(0.0, h.toDouble())
    if (c !== parent.getComponent(0)) {
      p.lineTo(w2, h2)
    }
    p.closePath()
    return AffineTransform.getTranslateInstance(x.toDouble(), y.toDouble()).createTransformedShape(p)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val parent = c.parent ?: return
    shape = makeShape(parent, c, x, y)
    var bgc = parent.background
    var borderColor = Color.GRAY.brighter()
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isRollover) {
        bgc = c.getBackground()
        borderColor = Color.GRAY
      }
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = bgc
    g2.fill(shape)
    g2.paint = borderColor
    g2.draw(shape)
    g2.dispose()
  }

  override fun getIconWidth() = WIDTH

  override fun getIconHeight() = HEIGHT

  companion object {
    const val TH = 10 // The height of a triangle
    private const val HEIGHT = TH * 2 + 1
    private const val WIDTH = 100
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
