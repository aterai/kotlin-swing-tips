package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import javax.swing.*
import kotlin.math.sqrt

fun makeUI() = JPanel().also {
  val roundIcon = ToggleButtonBarCellIcon()
  val rectIcon = CellIcon()
  it.add(makeToggleButtonBar(0xFF_74_00, roundIcon))
  it.add(makeToggleButtonBar(0x55_55_55, rectIcon))
  it.add(makeToggleButtonBar(0x00_64_00, roundIcon))
  it.add(makeToggleButtonBar(0x8B_00_00, rectIcon))
  it.add(makeToggleButtonBar(0x00_1E_43, roundIcon))
  it.preferredSize = Dimension(320, 240)
}

private fun makeButton(title: String) = JRadioButton(title).also {
  it.horizontalTextPosition = SwingConstants.CENTER
  it.border = BorderFactory.createEmptyBorder()
  it.isContentAreaFilled = false
  it.isFocusPainted = false
  it.foreground = Color.WHITE
}

private fun makeToggleButtonBar(
  cc: Int,
  icon: Icon,
): Component {
  val p = JPanel(GridLayout(1, 0, 0, 0))
  p.border = BorderFactory.createTitledBorder("Color: #%06X".format(cc))

  val bg = ButtonGroup()
  val color = Color(cc)
  listOf("left", "center", "right")
    .map { makeButton(it) }
    .forEach {
      it.background = color
      it.icon = icon
      bg.add(it)
      p.add(it)
    }
  return p
}

private class CellIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    if (c is AbstractButton) {
      var ssc = TL
      var bgc = BR
      val m = c.model
      if (m.isSelected || m.isRollover) {
        ssc = ST
        bgc = SB
      }
      val w = c.width
      val h = c.height
      g2.paint = c.background
      g2.fillRect(0, 0, w, h)
      g2.paint = GradientPaint(0f, 0f, ssc, 0f, h.toFloat(), bgc, true)
      g2.fillRect(0, 0, w, h)
      g2.paint = TL
      g2.fillRect(0, 0, 1, h)
      g2.paint = BR
      g2.fillRect(w, 0, 1, h)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 80

  override fun getIconHeight() = 20

  companion object {
    private val TL = Color(1f, 1f, 1f, .2f)
    private val BR = Color(0f, 0f, 0f, .2f)
    private val ST = Color(1f, 1f, 1f, .4f)
    private val SB = Color(1f, 1f, 1f, .1f)
  }
}

private class ToggleButtonBarCellIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    var ssc = TL
    var bgc = BR
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isRollover) {
        ssc = ST
        bgc = SB
      }
      val path = makeButtonPath(c, x, y) ?: return
      val area = Area(path)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = c.background
      g2.fill(area)
      g2.paint = GradientPaint(
        x.toFloat(),
        y.toFloat(),
        ssc,
        x.toFloat(),
        y.toFloat() + c.height,
        bgc,
        true,
      )
      g2.fill(area)
      g2.paint = BR
      g2.draw(area)
      g2.dispose()
    }
  }

  private fun makeButtonPath(
    c: Component?,
    x: Int,
    y: Int,
  ): Path2D.Double? {
    val parent = c?.parent ?: return null
    val r = 4.0
    val rr = r * 4.0 * (sqrt(2.0) - 1.0) / 3.0
    val dx = x.toDouble()
    val dy = y.toDouble()
    var dw = c.width.toDouble()
    val dh = c.height - 1.0

    val p = Path2D.Double()
    when {
      c === parent.getComponent(0) -> { // :first-child
        p.moveTo(dx, dy + r)
        p.curveTo(dx, dy + r - rr, dx + r - rr, dy, dx + r, dy)
        p.lineTo(dx + dw, dy)
        p.lineTo(dx + dw, dy + dh)
        p.lineTo(dx + r, dy + dh)
        p.curveTo(dx + r - rr, dy + dh, dx, dy + dh - r + rr, dx, dy + dh - r)
      }

      c === parent.getComponent(parent.componentCount - 1) -> { // :last-child
        dw--
        p.moveTo(dx, dy)
        p.lineTo(dx + dw - r, dy)
        p.curveTo(dx + dw - r + rr, dy, dx + dw, dy + r - rr, dx + dw, dy + r)
        p.lineTo(dx + dw, dy + dh - r)
        p.curveTo(dx + dw, dy + dh - r + rr, dx + dw - r + rr, dy + dh, dx + dw - r, dy + dh)
        p.lineTo(dx, dy + dh)
      }

      else -> {
        p.moveTo(dx, dy)
        p.lineTo(dx + dw, dy)
        p.lineTo(dx + dw, dy + dh)
        p.lineTo(dx, dy + dh)
      }
    }
    p.closePath()
    return p
  }

  override fun getIconWidth() = 80

  override fun getIconHeight() = 20

  companion object {
    private val TL = Color(1f, 1f, 1f, .2f)
    private val BR = Color(0f, 0f, 0f, .2f)
    private val ST = Color(1f, 1f, 1f, .4f)
    private val SB = Color(1f, 1f, 1f, .1f)
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
