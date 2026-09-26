package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun createUI(): Component {
  val button1 = object : JButton("RoundedCornerButtonUI") {
    override fun updateUI() {
      // IGNORE LnF change: super.updateUI()
      setUI(RoundedCornerButtonUI())
    }
  }
  val cl = Thread.currentThread().contextClassLoader
  val icon = cl.getResource("example/16x16.png")?.let { ImageIcon(it) }
    ?: UIManager.getIcon("html.missingImage")
  val button2 = object : RoundButton(icon) {
    override fun getPreferredSize(): Dimension {
      val margin = 4
      val s = maxOf(icon.iconWidth, icon.iconHeight)
      val size = s + (FOCUS_STROKE + margin) * 2
      return Dimension(size, size)
    }
  }

  return JPanel().also {
    it.add(JButton("Default JButton"))
    it.add(button1)
    it.add(RoundedCornerButton("Rounded Corner Button"))
    it.add(button2)
    it.add(ShapeButton(createStar(30.0, 25.0, 20)))
    it.add(RoundButton("Round Button"))
    it.preferredSize = Dimension(320, 240)
  }
}

fun createStar(
  outerRadius: Double,
  innerRadius: Double,
  vertexCount: Int,
): Shape {
  val step = PI / vertexCount
  var angle = -PI / 2.0 // start from the top vertex
  val p = Path2D.Double()
  p.moveTo(outerRadius * cos(angle), outerRadius * sin(angle))
  for (i in 1..<vertexCount * 2) {
    angle += step
    val r = if (i % 2 == 0) outerRadius else innerRadius
    p.lineTo(r * cos(angle), r * sin(angle))
  }
  p.closePath()
  val b = p.bounds2D
  val at = AffineTransform.getTranslateInstance(-b.x, -b.y)
  return at.createTransformedShape(p)
}

open class RoundedCornerButton : JButton {
  private val cachedSize = Dimension()
  private var shape: Shape? = null
  private var innerShape: Shape? = null

  constructor(icon: Icon) : super(icon)

  constructor(text: String) : super(text)

  override fun updateUI() {
    super.updateUI()
    isContentAreaFilled = false
    isFocusPainted = false
    background = Color(0xFA_FA_FA)
  }

  protected open fun createShape(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
  ): Shape = RoundRectangle2D.Double(x, y, w, h, ARC, ARC)

  private fun updateShapeIfResized(): Shape {
    val s0 = shape
    if (s0 != null && cachedSize == size) {
      return s0
    }
    getSize(cachedSize)
    val w = width - 1.0
    val h = height - 1.0
    val s = FOCUS_STROKE.toDouble()
    innerShape = createShape(s, s, w - s * 2.0, h - s * 2.0)
    return createShape(0.0, 0.0, w, h).also { shape = it }
  }

  private fun paintFocusAndRollover(
    g2: Graphics2D,
    color: Color,
  ) {
    val x2 = width - 1f
    val y2 = height - 1f
    g2.paint = GradientPaint(0f, 0f, color, x2, y2, color.brighter(), true)
    g2.fill(shape)
    g2.paint = background
    g2.fill(innerShape)
  }

  override fun paintComponent(g: Graphics) {
    val s = updateShapeIfResized()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val m = model
    when {
      m.isArmed -> {
        g2.paint = PRESSED_COLOR
        g2.fill(s)
      }

      isRolloverEnabled && m.isRollover -> paintFocusAndRollover(g2, ROLLOVER_COLOR)

      hasFocus() -> paintFocusAndRollover(g2, FOCUS_COLOR)

      else -> {
        g2.paint = background
        g2.fill(s)
      }
    }
    g2.dispose()
    super.paintComponent(g)
  }

  override fun paintBorder(g: Graphics) {
    val s = updateShapeIfResized()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = foreground
    g2.draw(s)
    g2.dispose()
  }

  override fun contains(
    x: Int,
    y: Int,
  ) = updateShapeIfResized().contains(x.toDouble(), y.toDouble())

  companion object {
    const val FOCUS_STROKE = 2
    val FOCUS_COLOR = Color(0xC8_64_96_FF.toInt(), true)
    val PRESSED_COLOR = Color(0xE6_E6_E6)
    val ROLLOVER_COLOR: Color = Color.ORANGE
    private const val ARC = 16.0
  }
}

open class RoundButton : RoundedCornerButton {
  constructor(icon: Icon) : super(icon)

  constructor(text: String) : super(text)

  override fun getPreferredSize() =
    super.getPreferredSize()?.also {
      val s = maxOf(it.width, it.height)
      it.setSize(s, s)
    }

  override fun createShape(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
  ): Shape = Ellipse2D.Double(x, y, w, h)
}

class ShapeButton(
  s: Shape,
) : JButton("Shape", ShapeSizeIcon(s)) {
  private val shape: Shape? = s

  override fun updateUI() {
    super.updateUI()
    verticalAlignment = CENTER
    verticalTextPosition = CENTER
    horizontalAlignment = CENTER
    horizontalTextPosition = CENTER
    border = BorderFactory.createEmptyBorder()
    isContentAreaFilled = false
    isFocusPainted = false
    background = Color(0xFA_FA_FA)
  }

  private fun paintFocusAndRollover(
    g2: Graphics2D,
    color: Color,
  ) {
    val x2 = width - 1f
    val y2 = height - 1f
    g2.paint = GradientPaint(0f, 0f, color, x2, y2, color.brighter(), true)
    g2.fill(shape)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val m = getModel()
    when {
      m.isArmed -> {
        g2.paint = PRESSED_COLOR
        g2.fill(shape)
      }

      isRolloverEnabled && m.isRollover -> paintFocusAndRollover(g2, ROLLOVER_COLOR)

      hasFocus() -> paintFocusAndRollover(g2, FOCUS_COLOR)

      else -> {
        g2.paint = background
        g2.fill(shape)
      }
    }
    g2.dispose()
    super.paintComponent(g)
  }

  override fun paintBorder(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = foreground
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(
    x: Int,
    y: Int,
  ) = shape?.contains(x.toDouble(), y.toDouble()) ?: super.contains(x, y)

  companion object {
    private val FOCUS_COLOR = Color(0xC8_64_96_FF.toInt(), true)
    private val PRESSED_COLOR = Color(0xE6_E6_E6)
    private val ROLLOVER_COLOR = Color.ORANGE
  }
}

class ShapeSizeIcon(
  shape: Shape,
) : Icon {
  private val bounds = shape.bounds

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // Empty icon
  }

  override fun getIconWidth() = bounds.x + bounds.width + 1

  override fun getIconHeight() = bounds.y + bounds.height + 1
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
