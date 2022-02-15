package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.cos
import kotlin.math.sin

private val button = object : JButton("RoundedCornerButtonUI") {
  override fun updateUI() {
    // IGNORE LnF change: super.updateUI()
    setUI(RoundedCornerButtonUI())
  }
}

fun makeUI() = JPanel().also {
  it.add(JButton("Default JButton"))
  // button.ui = RoundedCornerButtonUI()
  it.add(button)
  it.add(RoundedCornerButton("Rounded Corner Button"))
  val cl = Thread.currentThread().contextClassLoader
  val button = object : RoundButton(ImageIcon(cl.getResource("example/16x16.png"))) {
    override fun getPreferredSize() = super.getPreferredSize()?.also { d ->
      val r = 16 + (FOCUS_STROKE.toInt() + 4) * 2 // test margin = 4
      d.setSize(r, r)
    }
  }
  it.add(button)
  it.add(ShapeButton(makeStar(25, 30, 20)))
  it.add(RoundButton("Round Button"))
  it.preferredSize = Dimension(320, 240)
}

fun makeStar(r1: Int, r2: Int, vc: Int): Path2D {
  val ora = maxOf(r1, r2)
  val ira = minOf(r1, r2)
  var agl = 0.0
  val add = 2 * Math.PI / (vc * 2)
  val p = Path2D.Double()
  p.moveTo(ora * 1.0, ora * 0.0)
  for (i in 0 until vc * 2 - 1) {
    agl += add
    val r = if (i % 2 == 0) ira else ora
    p.lineTo(r * cos(agl), r * sin(agl))
  }
  p.closePath()
  val at = AffineTransform.getRotateInstance(-Math.PI / 2.0, ora.toDouble(), 0.0)
  return Path2D.Double(p, at)
}

open class RoundedCornerButton : JButton {
  private val fc = Color(100, 150, 255, 200)
  private val ac = Color(230, 230, 230)
  private val rc = Color.ORANGE
  protected var shape: Shape? = null
  protected var border: Shape? = null
  protected var base: Shape? = null

  // constructor() : super()

  constructor(icon: Icon) : super(icon)

  constructor(text: String) : super(text)

  // constructor(a: Action) : super(a)

  // constructor(text: String, icon: Icon) : super(text, icon)
  // {
  //   // setModel(new DefaultButtonModel());
  //   // init(text, icon);
  //   // setContentAreaFilled(false);
  //   // setBackground(new Color(250, 250, 250));
  //   // initShape();
  // }

  override fun updateUI() {
    super.updateUI()
    isContentAreaFilled = false
    isFocusPainted = false
    background = Color(250, 250, 250)
    initShape()
  }

  open fun initShape() {
    if (bounds != base) {
      base = bounds
      shape = RoundRectangle2D.Double(0.0, 0.0, width - 1.0, height - 1.0, ARC_WIDTH, ARC_HEIGHT)
      border = RoundRectangle2D.Double(
        FOCUS_STROKE,
        FOCUS_STROKE,
        width - 1 - FOCUS_STROKE * 2,
        height - 1 - FOCUS_STROKE * 2,
        ARC_WIDTH,
        ARC_HEIGHT
      )
    }
  }

  private fun paintFocusAndRollover(g2: Graphics2D, color: Color) {
    g2.paint = GradientPaint(0f, 0f, color, width - 1f, height - 1f, color.brighter(), true)
    g2.fill(shape)
    g2.paint = background
    g2.fill(border)
  }

  override fun paintComponent(g: Graphics) {
    initShape()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (getModel().isArmed) {
      g2.paint = ac
      g2.fill(shape)
    } else if (isRolloverEnabled && getModel().isRollover) {
      paintFocusAndRollover(g2, rc)
    } else if (hasFocus()) {
      paintFocusAndRollover(g2, fc)
    } else {
      g2.paint = background
      g2.fill(shape)
    }
    g2.dispose()
    super.paintComponent(g)
  }

  override fun paintBorder(g: Graphics) {
    initShape()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = foreground
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(x: Int, y: Int): Boolean {
    initShape()
    return shape?.contains(x.toDouble(), y.toDouble()) ?: false
  }

  companion object {
    private const val ARC_WIDTH = 16.0
    private const val ARC_HEIGHT = 16.0
    const val FOCUS_STROKE = 2.0
  }
}

open class RoundButton : RoundedCornerButton {
  // constructor() : super()

  constructor(icon: Icon) : super(icon)

  constructor(text: String) : super(text)

  // constructor(a: Action) : super(a)

  // constructor(text: String, icon: Icon) : super(text, icon)
  // {
  //   // setModel(DefaultButtonModel())
  //   // init(text, icon);
  // }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    val s = maxOf(width, height)
    it.setSize(s, s)
  }

  override fun initShape() {
    if (bounds != base) {
      base = bounds
      shape = Ellipse2D.Double(0.0, 0.0, width - 1.0, height - 1.0)
      border = Ellipse2D.Double(
        FOCUS_STROKE,
        FOCUS_STROKE,
        width - 1 - FOCUS_STROKE * 2,
        height - 1 - FOCUS_STROKE * 2
      )
    }
  }
}

class ShapeButton(private val shape: Shape) : JButton() {
  private val fc = Color(100, 150, 255, 200)
  private val ac = Color(230, 230, 230)
  private val rc = Color.ORANGE

  init {
    setModel(DefaultButtonModel())
    init("Shape", DummySizeIcon(shape))
    verticalAlignment = SwingConstants.CENTER
    verticalTextPosition = SwingConstants.CENTER
    horizontalAlignment = SwingConstants.CENTER
    horizontalTextPosition = SwingConstants.CENTER
    border = BorderFactory.createEmptyBorder()
    isContentAreaFilled = false
    isFocusPainted = false
    background = Color(250, 250, 250)
  }

  private fun paintFocusAndRollover(g2: Graphics2D, color: Color) {
    g2.paint = GradientPaint(0f, 0f, color, width - 1f, height - 1f, color.brighter(), true)
    g2.fill(shape)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (getModel().isArmed) {
      g2.paint = ac
      g2.fill(shape)
    } else if (isRolloverEnabled && getModel().isRollover) {
      paintFocusAndRollover(g2, rc)
    } else if (hasFocus()) {
      paintFocusAndRollover(g2, fc)
    } else {
      g2.paint = background
      g2.fill(shape)
    }
    g2.dispose()
    super.paintComponent(g)
  }

  override fun paintBorder(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = foreground
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(x: Int, y: Int) = shape.contains(x.toDouble(), y.toDouble())
}

class DummySizeIcon(private val shape: Shape) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    /* Empty icon */
  }

  override fun getIconWidth() = shape.bounds.width

  override fun getIconHeight() = shape.bounds.height
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
