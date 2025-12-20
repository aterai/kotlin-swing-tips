package example

import java.awt.*
import javax.swing.*
import javax.swing.border.BevelBorder
import javax.swing.border.Border

fun makeUI(): Component {
  val p = JPanel(FlowLayout(FlowLayout.CENTER, 20, 20))
  p.border = BorderFactory.createEmptyBorder(20, 50, 20, 50)

  val border1 = object : BevelBorder(RAISED) {
    override fun getBorderInsets(
      c: Component,
      insets: Insets,
    ): Insets {
      insets.set(10, 10, 10, 10)
      return insets
    }
  }
  p.add(makeButton("Default BevelBorder", border1))

  val border2 = CustomBevelBorder(BevelBorder.RAISED)
  p.add(makeButton("Custom BevelBorder", border2))

  EventQueue.invokeLater { SwingUtilities.updateComponentTreeUI(p) }
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton(
  text: String,
  btnBorder: Border,
) = object : JButton("<html>JButton<br>+ $text") {
  override fun updateUI() {
    super.updateUI()
    isOpaque = true
    foreground = Color.WHITE
    background = Color(0x5B_9B_D5)
    isFocusPainted = false
    isContentAreaFilled = false
    border = btnBorder
  }
}

private class CustomBevelBorder(
  bevelType: Int,
) : BevelBorder(bevelType) {
  private val ins = Insets(8, 8, 8, 8)

  override fun getBorderInsets(
    c: Component,
    insets: Insets,
  ): Insets {
    insets.set(ins.top + 2, ins.left + 2, ins.bottom + 2, ins.right + 2)
    return insets
  }

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    var isPressed = false
    if (c is AbstractButton) {
      val m = c.model
      isPressed = m.isPressed
    }
    if (bevelType == RAISED && !isPressed) {
      paintRaisedBevel(c, g, x, y, width, height)
    } else { // if (bevelType == LOWERED) {
      paintLoweredBevel(c, g, x, y, width, height)
    }
  }

  override fun paintRaisedBevel(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    val w = width - 1
    val h = height - 1
    g2.paint = getHighlightInnerColor(c)
    fillTopLeft(g2, w, h, ins)
    g2.paint = getShadowInnerColor(c)
    g2.fill(makeBottomRightShape(w, h, ins))
    g2.paint = getShadowOuterColor(c)
    drawRectLine(g2, w, h, ins)
    g2.dispose()
  }

  override fun paintLoweredBevel(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    val w = width - 1
    val h = height - 1
    g2.paint = getShadowInnerColor(c)
    fillTopLeft(g2, w, h, ins)
    g2.paint = getHighlightInnerColor(c)
    g2.fill(makeBottomRightShape(w, h, ins))
    g2.paint = getShadowOuterColor(c)
    drawRectLine(g2, w, h, ins)
    g2.dispose()
  }

  private fun fillTopLeft(
    g2: Graphics2D,
    w: Int,
    h: Int,
    i: Insets,
  ) {
    g2.fillRect(0, 0, w, i.top)
    g2.fillRect(0, 0, i.left, h)
  }

  private fun makeBottomRightShape(
    w: Int,
    h: Int,
    i: Insets,
  ): Shape {
    val p = Polygon()
    p.addPoint(w, 0)
    p.addPoint(w - i.right, i.top)
    p.addPoint(w - i.right, h - i.bottom)
    p.addPoint(i.left, h - i.bottom)
    p.addPoint(0, h)
    p.addPoint(w, h)
    return p
  }

  private fun drawRectLine(
    g2: Graphics2D,
    w: Int,
    h: Int,
    i: Insets,
  ) {
    g2.drawRect(0, 0, w, h)
    g2.drawRect(i.left, i.top, w - i.left - i.right, h - i.top - i.bottom)
    g2.drawLine(0, 0, i.left, i.top)
    g2.drawLine(w, 0, w - i.right, i.top)
    g2.drawLine(0, h, i.left, h - i.bottom)
    g2.drawLine(w, h, w - i.right, h - i.bottom)
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
