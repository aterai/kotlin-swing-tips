package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import javax.swing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun createUI() = JPanel(GridLayout(2, 3)).also {
  it.add(makeTitledPanel("GeneralPath", StarPanel1()))
  it.add(makeTitledPanel("Polygon", StarPanel2()))
  it.add(makeTitledPanel("Font(Outline)", StarPanel3()))
  it.add(makeTitledPanel("Icon", JLabel(StarIcon0())))
  it.add(makeTitledPanel("Icon(R=40)", JLabel(StarIcon1())))
  it.add(makeTitledPanel("Icon(R=20,40)", JLabel(StarIcon2())))
  it.preferredSize = Dimension(320, 240)
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class StarPanel1 : JPanel() {
  override fun paintComponent(g: Graphics) {
    val w = width
    val h = height
    // %JAVA_HOME%/demo/jfc/Java2D/src/java2d/demos/Lines/Joins.java
    val p = GeneralPath()
    p.moveTo(-w / 4f, -h / 12f)
    p.lineTo(w / 4f, -h / 12f)
    p.lineTo(-w / 6f, h / 4f)
    p.lineTo(0f, -h / 4f)
    p.lineTo(w / 6f, h / 4f)
    p.closePath()

    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(w / 2, h / 2)
    g2.paint = Color.YELLOW
    g2.fill(p)
    g2.paint = Color.BLACK
    g2.draw(p)
    g2.dispose()
  }
}

private class StarPanel2 : JPanel() {
  override fun paintComponent(g: Graphics) {
    val w = width
    val h = height
    val p = Polygon()
    p.addPoint((-w / 4f).roundToInt(), (-h / 12f).roundToInt())
    p.addPoint((w / 4f).roundToInt(), (-h / 12f).roundToInt())
    p.addPoint((-w / 6f).roundToInt(), (h / 4f).roundToInt())
    p.addPoint(0, (-h / 4f).roundToInt())
    p.addPoint((w / 6f).roundToInt(), (h / 4f).roundToInt())

    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(w / 2, h / 2)
    g2.paint = Color.YELLOW
    g2.fill(p)
    g2.paint = Color.BLACK
    g2.draw(p)
    g2.dispose()
  }
}

private class StarPanel3 : JPanel() {
  private val font = Font(Font.SERIF, Font.PLAIN, FONT_SIZE)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(0, FONT_SIZE)
    val frc = g2.fontRenderContext
    val shape = TextLayout("★", font, frc).getOutline(null)
    g2.paint = Color.YELLOW
    g2.fill(shape)
    g2.paint = Color.BLACK
    g2.draw(shape)
    g2.dispose()
  }

  companion object {
    private const val FONT_SIZE = 80
  }
}

private class StarIcon0 : Icon {
  private val path = GeneralPath()

  init {
    // https://gihyo.jp/dev/serial/01/javafx/0009?page=2
    path.moveTo(50.000, 0.0000)
    path.lineTo(61.803, 38.196)
    path.lineTo(100.00, 38.196)
    path.lineTo(69.098, 61.804)
    path.lineTo(80.902, 100.00)
    path.lineTo(50.000, 76.394)
    path.lineTo(19.098, 100.00)
    path.lineTo(30.902, 61.804)
    path.lineTo(0.0000, 38.196)
    path.lineTo(38.197, 38.196)
    path.closePath()
    // Scale the 100x100 path down to 80x80 at once instead of scaling each coordinate
    path.transform(AffineTransform.getScaleInstance(SCALE, SCALE))
  }

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.YELLOW
    g2.fill(path)
    g2.paint = Color.BLACK
    g2.draw(path)
    g2.dispose()
  }

  override fun getIconWidth() = 80

  override fun getIconHeight() = 80

  companion object {
    private const val SCALE = .8
  }
}

private class StarIcon1 : Icon {
  private val star: Shape

  init {
    // Connect every second vertex of a regular pentagon (step = 4 * PI / 5)
    val step = 4.0 * PI / 5.0
    val path = Path2D.Double()
    path.moveTo(RADIUS.toDouble(), 0.0)
    for (i in 1..<5) {
      val angle = step * i
      path.lineTo(RADIUS * cos(angle), RADIUS * sin(angle))
    }
    path.closePath()
    // Rotate -90 degrees around (RADIUS, 0) so that the first vertex points up and
    // the center of the star moves from (0, 0) to (RADIUS, RADIUS)
    val at = AffineTransform.getRotateInstance(-PI / 2.0, RADIUS.toDouble(), 0.0)
    star = Path2D.Double(path, at)
  }

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.YELLOW
    g2.fill(star)
    g2.paint = Color.BLACK
    g2.draw(star)
    g2.dispose()
  }

  override fun getIconWidth() = 2 * RADIUS

  override fun getIconHeight() = 2 * RADIUS

  companion object {
    private const val RADIUS = 40
  }
}

private class StarIcon2 : Icon {
  private val star: Shape

  init {
    val step = PI / VERTEX_COUNT
    val path = Path2D.Double()
    path.moveTo(OUTER_RADIUS.toDouble(), 0.0)
    for (i in 1..<VERTEX_COUNT * 2) {
      // odd indices are the inner (concave) vertices
      val r = if (i % 2 == 0) OUTER_RADIUS else INNER_RADIUS
      val angle = step * i
      path.lineTo(r * cos(angle), r * sin(angle))
    }
    path.closePath()
    val at = AffineTransform.getRotateInstance(-PI / 2.0, OUTER_RADIUS.toDouble(), 0.0)
    star = Path2D.Double(path, at)
  }

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.YELLOW
    g2.fill(star)
    g2.paint = Color.BLACK
    g2.draw(star)
    g2.dispose()
  }

  override fun getIconWidth() = 2 * OUTER_RADIUS

  override fun getIconHeight() = 2 * OUTER_RADIUS

  companion object {
    private const val OUTER_RADIUS = 40
    private const val INNER_RADIUS = 20

    // Inner radius of a regular pentagram:
    // OUTER_RADIUS * cos(2.0 * PI / 5.0) / cos(PI / 5.0) // = 15.28...
    private const val VERTEX_COUNT = 5 // 16
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
