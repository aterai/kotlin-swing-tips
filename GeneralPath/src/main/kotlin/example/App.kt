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

fun makeUI() = JPanel(GridLayout(2, 3)).also {
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
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(0, FONT_SIZE)
    val frc = g2.fontRenderContext
    val font = Font(Font.SERIF, Font.PLAIN, FONT_SIZE)
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
    path.moveTo(50.000 * .8, 0.0000 * .8)
    path.lineTo(61.803 * .8, 38.196 * .8)
    path.lineTo(100.00 * .8, 38.196 * .8)
    path.lineTo(69.098 * .8, 61.804 * .8)
    path.lineTo(80.902 * .8, 100.00 * .8)
    path.lineTo(50.000 * .8, 76.394 * .8)
    path.lineTo(19.098 * .8, 100.00 * .8)
    path.lineTo(30.902 * .8, 61.804 * .8)
    path.lineTo(0.0000 * .8, 38.196 * .8)
    path.lineTo(38.197 * .8, 38.196 * .8)
    path.closePath()
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
}

private class StarIcon1 : Icon {
  private val star: Shape

  init {
    var agl = 0.0
    val add = 2.0 * PI / 5.0
    val p = Path2D.Double()
    p.moveTo(R.toDouble(), 0.0)
    for (i in 0..4) {
      p.lineTo(R * cos(agl), R * sin(agl))
      agl += add + add
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-PI / 2.0, R.toDouble(), 0.0)
    star = Path2D.Double(p, at)
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

  override fun getIconWidth() = 2 * R

  override fun getIconHeight() = 2 * R

  companion object {
    private const val R = 40
  }
}

private class StarIcon2 : Icon {
  private val star: Shape

  init {
    var agl = 0.0
    val add = PI / VC
    val p = Path2D.Double()
    p.moveTo(R2.toDouble(), 0.0)
    for (i in 0..<VC * 2 - 1) {
      agl += add
      val r = if (i % 2 == 0) R1 else R2
      p.lineTo(r * cos(agl), r * sin(agl))
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-PI / 2.0, R2.toDouble(), 0.0)
    star = Path2D.Double(p, at)
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

  override fun getIconWidth() = 2 * R2

  override fun getIconHeight() = 2 * R2

  companion object {
    private const val R2 = 40
    private const val R1 = 20
    private const val VC = 5 // 16
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
