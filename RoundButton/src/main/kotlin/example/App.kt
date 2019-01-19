package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.util.Optional
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  private val button = object : JButton("RoundedCornerButtonUI") {
    override fun updateUI() {
      // IGNORE LnF change: super.updateUI();
      setUI(RoundedCornerButtonUI())
    }
  }

  init {
    add(JButton("Default JButton"))
    // button.setUI(new RoundedCornerButtonUI());
    add(button)
    add(RoundedCornerButton("Rounded Corner Button"))
    add(object : RoundButton(ImageIcon(MainPanel::class.java.getResource("16x16.png"))) {
      override fun getPreferredSize(): Dimension {
        val r = 16 + (RoundedCornerButton.FOCUS_STROKE.toInt() + 4) * 2 // test margin = 4
        return Dimension(r, r)
      }
    })
    add(ShapeButton(makeStar(25, 30, 20)))
    add(RoundButton("Round Button"))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeStar(r1: Int, r2: Int, vc: Int): Path2D {
    val ora = Math.max(r1, r2)
    val ira = Math.min(r1, r2)
    var agl = 0.0
    val add = 2 * Math.PI / (vc * 2)
    val p = Path2D.Double()
    p.moveTo(ora * 1.0, ora * 0.0)
    for (i in 0 until vc * 2 - 1) {
      agl += add
      val r = if (i % 2 == 0) ira else ora
      p.lineTo(r * Math.cos(agl), r * Math.sin(agl))
    }
    p.closePath()
    val at = AffineTransform.getRotateInstance(-Math.PI / 2.0, ora.toDouble(), 0.0)
    return Path2D.Double(p, at)
  }
}

open class RoundedCornerButton : JButton {
  protected val fc = Color(100, 150, 255, 200)
  protected val ac = Color(230, 230, 230)
  protected val rc = Color.ORANGE
  protected var shape: Shape? = null
  protected var border: Shape? = null
  protected var base: Shape? = null

  public constructor() : super() {}

  public constructor(icon: Icon) : super(icon) {}

  public constructor(text: String) : super(text) {}

  public constructor(a: Action) : super(a) {
    // setAction(a);
  }

  public constructor(text: String, icon: Icon) : super(text, icon) {
    // setModel(new DefaultButtonModel());
    // init(text, icon);
    // setContentAreaFilled(false);
    // setBackground(new Color(250, 250, 250));
    // initShape();
  }

  override fun updateUI() {
    super.updateUI()
    setContentAreaFilled(false)
    setFocusPainted(false)
    setBackground(Color(250, 250, 250))
    initShape()
  }

  open fun initShape() {
    if (getBounds() != base) {
      base = getBounds()
      shape = RoundRectangle2D.Double(0.0, 0.0, getWidth() - 1.0, getHeight() - 1.0, ARC_WIDTH, ARC_HEIGHT)
      border = RoundRectangle2D.Double(
          FOCUS_STROKE, FOCUS_STROKE,
          getWidth() - 1 - FOCUS_STROKE * 2, getHeight() - 1 - FOCUS_STROKE * 2,
          ARC_WIDTH, ARC_HEIGHT)
    }
  }

  private fun paintFocusAndRollover(g2: Graphics2D, color: Color) {
    g2.setPaint(GradientPaint(0f, 0f, color, getWidth() - 1f, getHeight() - 1f, color.brighter(), true))
    g2.fill(shape)
    g2.setPaint(getBackground())
    g2.fill(border)
  }

  protected override fun paintComponent(g: Graphics) {
    initShape()
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (getModel().isArmed()) {
      g2.setPaint(ac)
      g2.fill(shape)
    } else if (isRolloverEnabled() && getModel().isRollover()) {
      paintFocusAndRollover(g2, rc)
    } else if (hasFocus()) {
      paintFocusAndRollover(g2, fc)
    } else {
      g2.setPaint(getBackground())
      g2.fill(shape)
    }
    g2.dispose()
    super.paintComponent(g)
  }

  protected override fun paintBorder(g: Graphics) {
    initShape()
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(getForeground())
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(x: Int, y: Int): Boolean {
    initShape()
    return shape?.let { it.contains(x.toDouble(), y.toDouble()) } ?: false
  }

  companion object {
    private val ARC_WIDTH = 16.0
    private val ARC_HEIGHT = 16.0
    val FOCUS_STROKE = 2.0
  }
}

open class RoundButton : RoundedCornerButton {
  public constructor() : super() {}

  public constructor(icon: Icon) : super(icon) {}

  public constructor(text: String) : super(text) {}

  public constructor(a: Action) : super(a) {
    // setAction(a);
  }

  public constructor(text: String, icon: Icon) : super(text, icon) {
    // setModel(new DefaultButtonModel());
    // init(text, icon);
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    val s = Math.max(d.width, d.height)
    d.setSize(s, s)
    return d
  }

  override fun initShape() {
    if (getBounds() != base) {
      base = getBounds()
      shape = Ellipse2D.Double(0.0, 0.0, getWidth() - 1.0, getHeight() - 1.0)
      border = Ellipse2D.Double(
          RoundedCornerButton.FOCUS_STROKE,
          RoundedCornerButton.FOCUS_STROKE,
          getWidth() - 1 - RoundedCornerButton.FOCUS_STROKE * 2,
          getHeight() - 1 - RoundedCornerButton.FOCUS_STROKE * 2)
    }
  }
}

internal class ShapeButton(protected val shape: Shape) : JButton() {
  protected val fc = Color(100, 150, 255, 200)
  protected val ac = Color(230, 230, 230)
  protected val rc = Color.ORANGE

  init {
    setModel(DefaultButtonModel())
    init("Shape", DummySizeIcon(shape))
    setVerticalAlignment(SwingConstants.CENTER)
    setVerticalTextPosition(SwingConstants.CENTER)
    setHorizontalAlignment(SwingConstants.CENTER)
    setHorizontalTextPosition(SwingConstants.CENTER)
    setBorder(BorderFactory.createEmptyBorder())
    setContentAreaFilled(false)
    setFocusPainted(false)
    setBackground(Color(250, 250, 250))
  }

  private fun paintFocusAndRollover(g2: Graphics2D, color: Color) {
    g2.setPaint(GradientPaint(0f, 0f, color, (getWidth() - 1).toFloat(), (getHeight() - 1).toFloat(), color.brighter(), true))
    g2.fill(shape)
  }

  protected override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if (getModel().isArmed()) {
      g2.setPaint(ac)
      g2.fill(shape)
    } else if (isRolloverEnabled() && getModel().isRollover()) {
      paintFocusAndRollover(g2, rc)
    } else if (hasFocus()) {
      paintFocusAndRollover(g2, fc)
    } else {
      g2.setPaint(getBackground())
      g2.fill(shape)
    }
    g2.dispose()
    super.paintComponent(g)
  }

  protected override fun paintBorder(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(getForeground())
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(x: Int, y: Int) = shape.contains(x.toDouble(), y.toDouble())
}

internal class DummySizeIcon(private val shape: Shape) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    /* Empty icon */
  }

  override fun getIconWidth() = shape.getBounds().width

  override fun getIconHeight() = shape.getBounds().height
}

fun main() {
  EventQueue.invokeLater({
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  })
}
