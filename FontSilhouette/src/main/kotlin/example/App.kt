package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.io.Serializable
import javax.swing.*

private val SIZE = Dimension(50, 50)
private val FONT = Font(Font.SANS_SERIF, Font.PLAIN, SIZE.width)

fun makeUI(): Component {
  val pieces = arrayOf("♔", "♕", "♖", "♗", "♘", "♙", "♚", "♛", "♜", "♝", "♞", "♟")
  return JPanel(GridLayout(4, 6, 0, 0)).also {
    for (i in pieces.indices) {
      it.add(initLabel(JLabel(pieces[i]), i))
    }
    for (i in pieces.indices) {
      it.add(initLabel(makeIconLabel(pieces[i]), i))
    }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeIconLabel(txt: String) = JLabel(SilhouetteIcon(FONT, txt, SIZE))

private fun initLabel(
  l: JLabel,
  i: Int,
): JLabel {
  l.horizontalAlignment = SwingConstants.CENTER
  l.font = FONT
  l.isOpaque = true
  val isFirstHalf = i < 6
  val isEven = i % 2 == 0
  if (isFirstHalf == isEven) {
    l.foreground = Color.BLACK
    l.background = Color.WHITE
  } else {
    l.foreground = Color.WHITE
    l.background = Color.BLACK
  }
  return l
}

private class SilhouetteIcon(
  private val font: Font,
  private val str: String,
  private val size: Dimension,
) : Icon,
  Serializable {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val frc = g2.fontRenderContext
    val shape = font.createGlyphVector(frc, str).outline
    val r = shape.bounds
    val cx = iconWidth / 2.0 - r.centerX
    val cy = iconHeight / 2.0 - r.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(cx, cy)
    val shapeCentered = toCenterAt.createTransformedShape(shape)
    val silhouette = getOuterShape(shapeCentered)
    g2.stroke = BasicStroke(3f)
    g2.paint = c.foreground
    g2.draw(silhouette)
    g2.paint = PIECE_PAINT
    g2.fill(silhouette)
    g2.stroke = BasicStroke(1f)
    g2.paint = c.background
    g2.fill(shapeCentered)
    g2.dispose()
  }

  override fun getIconWidth() = size.width

  override fun getIconHeight() = size.height

  // Inspired from java - 'Fill' Unicode characters in labels - Stack Overflow
  // https://stackoverflow.com/questions/18686199/fill-unicode-characters-in-labels
  private fun getOuterShape(shape: Shape): Area {
    val area = Area()
    val path = Path2D.Double()
    val pi = shape.getPathIterator(null)
    val cd = DoubleArray(6)
    while (!pi.isDone) {
      when (val pathSegmentType = pi.currentSegment(cd)) {
        PathIterator.SEG_MOVETO -> path.moveTo(cd[0], cd[1])

        PathIterator.SEG_LINETO -> path.lineTo(cd[0], cd[1])

        PathIterator.SEG_QUADTO -> path.quadTo(cd[0], cd[1], cd[2], cd[3])

        PathIterator.SEG_CUBICTO -> path.curveTo(cd[0], cd[1], cd[2], cd[3], cd[4], cd[5])

        PathIterator.SEG_CLOSE -> path.also {
          it.closePath()
          area.add(createArea(it))
          it.reset()
        }

        else -> System.err.println("Unexpected value! $pathSegmentType")
      }
      pi.next()
    }
    return area
  }

  private fun createArea(path: Path2D) = Area(path)

  companion object {
    private const val serialVersionUID = 1L
    private val PIECE_PAINT = Color(0x96_64_14)
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
