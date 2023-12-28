package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import javax.swing.*

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
    val parent = c?.parent ?: return
    val r = 8f
    val fx = x.toFloat()
    val fy = y.toFloat()
    var fw = c.width.toFloat()
    val fh = c.height - 1f
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val p = Path2D.Float()
    when {
      c === parent.getComponent(0) -> { // :first-child
        p.moveTo(fx, y + r)
        p.quadTo(fx, fy, x + r, fy)
        p.lineTo(x + fw, fy)
        p.lineTo(x + fw, y + fh)
        p.lineTo(x + r, y + fh)
        p.quadTo(fx, y + fh, fx, y + fh - r)
      }
      c === parent.getComponent(parent.componentCount - 1) -> { // :last-child
        fw--
        p.moveTo(fx, fy)
        p.lineTo(x + fw - r, fy)
        p.quadTo(x + fw, fy, x + fw, y + r)
        p.lineTo(x + fw, y + fh - r)
        p.quadTo(x + fw, y + fh, x + fw - r, y + fh)
        p.lineTo(fx, y + fh)
      }
      else -> {
        p.moveTo(fx, fy)
        p.lineTo(x + fw, fy)
        p.lineTo(x + fw, y + fh)
        p.lineTo(fx, y + fh)
      }
    }
    p.closePath()
    var ssc = TL
    var bgc = BR
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isRollover) {
        ssc = ST
        bgc = SB
      }
    }
    val area = Area(p)
    g2.paint = c.background
    g2.fill(area)
    g2.paint = GradientPaint(fx, fy, ssc, fx, y + fh, bgc, true)
    g2.fill(area)
    g2.paint = BR
    g2.draw(area)
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
