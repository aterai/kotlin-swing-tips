package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import javax.swing.*

fun makeUI(): Component {
  val font = Font(Font.SERIF, Font.PLAIN, 64)

  val label1 = LineSplittingLabel("ABC")
  label1.font = font

  val label2 = TricoloreLabel("DEF")
  label2.font = font

  return JPanel(GridLayout(1, 2)).also {
    it.add(label1)
    it.add(label2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TricoloreLabel(
  private val text: String,
) : JComponent() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val w = width
    val h = height
    g.color = Color.WHITE
    g.fillRect(0, 0, w, h)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val frc = g2.fontRenderContext
    val gv = font.createGlyphVector(frc, text)
    val b = gv.visualBounds
    val cx = w / 2.0 - b.centerX
    val cy = h / 2.0 - b.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(cx, cy)
    val dh = b.height / 3.0
    val clip = Rectangle2D.Double(b.x, b.y, b.width, b.height)
    val clip1 = Rectangle2D.Double(b.x, b.y, b.width, dh)
    val clip2 = Rectangle2D.Double(b.x, b.y + 2.0 * dh, b.width, dh)
    val s = toCenterAt.createTransformedShape(gv.outline)
    g2.clip = toCenterAt.createTransformedShape(clip1)
    g2.paint = Color.BLUE
    g2.fill(s)
    g2.clip = toCenterAt.createTransformedShape(clip2)
    g2.paint = Color.RED
    g2.fill(s)
    g2.clip = toCenterAt.createTransformedShape(clip)
    g2.paint = Color.BLACK
    g2.draw(s)
    g2.dispose()
  }
}

private class LineSplittingLabel(
  private val text: String,
) : JComponent() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val w = width
    val h = height
    g.color = Color.WHITE
    g.fillRect(0, 0, w, h)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val frc = g2.fontRenderContext
    val shape = TextLayout(text, font, frc).getOutline(null)
    val b = shape.bounds2D
    val cx = w / 2.0 - b.centerX
    val cy = h / 2.0 - b.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(cx, cy)
    val s = toCenterAt.createTransformedShape(shape)
    g2.paint = Color.BLACK
    g2.fill(s)
    val clip = Rectangle2D.Double(b.x, b.y, b.width, b.height / 2.0)
    g2.clip = toCenterAt.createTransformedShape(clip)
    g2.paint = Color.RED
    g2.fill(s)
    g2.dispose()
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
