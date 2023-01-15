package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.time.LocalTime
import java.time.ZoneId
import java.util.EnumSet
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  return JPanel(GridLayout(2, 1)).also {
    it.add(DigitalClock())
    it.add(HelpPanel())
    it.preferredSize = Dimension(320, 240)
  }
}

private class DigitalClock : JPanel() {
  private val h1: DigitalNumber
  private val h2: DigitalNumber
  private val m1: DigitalNumber
  private val m2: DigitalNumber
  private val s1: DigitalNumber
  private val s2: DigitalNumber
  private val dot1: Shape
  private val dot2: Shape
  private var pulse = false
  private val timer = Timer(250) {
    updateTime()
    pulse = !pulse
    repaint()
  }
  private var listener: HierarchyListener? = null

  init {
    background = DigitalNumber.BGC
    var x = SIZE * 3.0
    val y = SIZE * 8.0
    val gap = SIZE * 1.5
    h1 = DigitalNumber(x, y, SIZE)
    val r = h1.bounds
    x += r.width + gap
    h2 = DigitalNumber(x, y, SIZE)
    x += r.width.toDouble()
    val sz = SIZE * 1.5
    dot1 = Ellipse2D.Double(x, r.centerY.toFloat() - gap, sz, sz)
    dot2 = Ellipse2D.Double(x, r.centerY.toFloat() + gap, sz, sz)
    x += sz + gap
    m1 = DigitalNumber(x, y, SIZE)
    x += r.width + gap
    m2 = DigitalNumber(x, y, SIZE)
    x += r.width + gap
    val hs = SIZE / 2.0
    val y2 = y + h1.bounds.height / 4.0
    s1 = DigitalNumber(x, y2, hs)
    x += s1.bounds.width + gap / 2.0
    s2 = DigitalNumber(x, y2, hs)
  }

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
        if (e.component.isShowing) {
          timer.start()
        } else {
          timer.stop()
        }
      }
    }
    addHierarchyListener(listener)
  }

  private fun updateTime() {
    val ten = 10
    val time = LocalTime.now(ZoneId.systemDefault())
    // set Hours
    val hours = time.hour
    if (hours < ten) {
      h1.turnOffNumber()
      h2.setNumber(hours)
    } else {
      val dh = hours / ten
      h1.setNumber(dh)
      h2.setNumber(hours - dh * ten)
    }
    // set Minutes
    val minutes = time.minute
    val dm = minutes / ten
    m1.setNumber(dm)
    m2.setNumber(minutes - dm * ten)
    // set Seconds
    val seconds = time.second
    val ds = seconds / ten
    s1.setNumber(ds)
    s2.setNumber(seconds - ds * ten)
  }

  public override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.stroke = BasicStroke(3f)
    g2.shear(-.1, 0.0)
    val sv = width / (h1.bounds.width * 8.0)
    g2.scale(sv, sv)
    h1.drawNumber(g2)
    h2.drawNumber(g2)
    g2.color = if (pulse) DigitalNumber.ON else DigitalNumber.OFF
    g2.fill(dot1)
    g2.fill(dot2)
    m1.drawNumber(g2)
    m2.drawNumber(g2)
    s1.drawNumber(g2)
    s2.drawNumber(g2)
    g2.dispose()
  }

  companion object {
    private const val SIZE = 16.0
  }
}

private class DigitalNumber(
  private val dx: Double,
  private val dy: Double,
  private val isosceles: Double
) {
  private val width: Double
  private val height: Double
  val bounds = Rectangle()
  private val numbers: List<Set<Seg>> = listOf(
    EnumSet.of(Seg.A, Seg.B, Seg.C, Seg.D, Seg.E, Seg.F),
    EnumSet.of(Seg.B, Seg.C),
    EnumSet.of(Seg.A, Seg.B, Seg.D, Seg.E, Seg.G),
    EnumSet.of(Seg.A, Seg.B, Seg.C, Seg.D, Seg.G),
    EnumSet.of(Seg.B, Seg.C, Seg.F, Seg.G),
    EnumSet.of(Seg.A, Seg.C, Seg.D, Seg.F, Seg.G),
    EnumSet.of(Seg.A, Seg.C, Seg.D, Seg.E, Seg.F, Seg.G),
    EnumSet.of(Seg.A, Seg.B, Seg.C),
    EnumSet.of(Seg.A, Seg.B, Seg.C, Seg.D, Seg.E, Seg.F, Seg.G),
    EnumSet.of(Seg.A, Seg.B, Seg.C, Seg.D, Seg.F, Seg.G),
  )
  private var led: Set<Seg> = EnumSet.noneOf(Seg::class.java)

  init {
    width = 2.0 * isosceles
    height = width + isosceles
    bounds.setLocation((dx - isosceles).toInt(), (dy - height * 2.0).toInt())
    bounds.setSize((width + 4.0 * isosceles).toInt(), (height * 4.0).toInt())
  }

  fun setNumber(num: Int) {
    led = numbers[num]
  }

  fun turnOffNumber() {
    led = EnumSet.noneOf(Seg::class.java)
  }

  fun drawNumber(g2: Graphics2D) {
    EnumSet.allOf(Seg::class.java).forEach { s ->
      g2.color = if (led.contains(s)) ON else OFF
      val seg = s.getShape(dx, dy, width, height, isosceles)
      g2.fill(seg)
      g2.color = BGC
      g2.draw(seg)
    }
  }

  companion object {
    val OFF = Color(0xCC_CC_CC)
    val ON = Color.DARK_GRAY
    val BGC = Color.LIGHT_GRAY
  }
}

private enum class Seg {
  A {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x, y - h - i * 2)
      return at.createTransformedShape(horiz2(w, i))
    }
  },
  B {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x + w + i * 2, y)
      at.scale(-1.0, 1.0)
      return at.createTransformedShape(vert(h, i))
    }
  },
  C {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x + w + i * 2, y)
      at.scale(-1.0, -1.0)
      return at.createTransformedShape(vert(h, i))
    }
  },
  D {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x, y + h + i * 2)
      at.scale(1.0, -1.0)
      return at.createTransformedShape(horiz2(w, i))
    }
  },
  E {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x, y)
      at.scale(1.0, -1.0)
      return at.createTransformedShape(vert(h, i))
    }
  },
  F {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x, y)
      return at.createTransformedShape(vert(h, i))
    }
  },
  G {
    override fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape {
      val at = AffineTransform.getTranslateInstance(x, y)
      return at.createTransformedShape(horiz1(w, i))
    }
  };

  abstract fun getShape(x: Double, y: Double, w: Double, h: Double, i: Double): Shape

  companion object {
    private fun vert(height: Double, isosceles: Double): Path2D {
      val path: Path2D = Path2D.Double()
      path.moveTo(0.0, 0.0)
      path.lineTo(isosceles, -isosceles)
      path.lineTo(isosceles, -isosceles - height)
      path.lineTo(-isosceles, -isosceles - height - isosceles * 2)
      path.lineTo(-isosceles, -isosceles)
      path.closePath()
      return path
    }

    private fun horiz1(width: Double, isosceles: Double): Path2D {
      val path: Path2D = Path2D.Double()
      path.moveTo(0.0, 0.0)
      path.lineTo(isosceles, isosceles)
      path.lineTo(isosceles + width, isosceles)
      path.lineTo(isosceles + width + isosceles, 0.0)
      path.lineTo(isosceles + width, -isosceles)
      path.lineTo(isosceles, -isosceles)
      path.closePath()
      return path
    }

    private fun horiz2(width: Double, isosceles: Double): Path2D {
      val path: Path2D = Path2D.Double()
      path.moveTo(isosceles, isosceles)
      path.lineTo(isosceles + width, isosceles)
      path.lineTo(3 * isosceles + width, -isosceles)
      path.lineTo(-isosceles, -isosceles)
      path.closePath()
      return path
    }
  }
}

private class HelpPanel : JPanel() {
  private val help = DigitalNumber(SIZE * 3.0, SIZE * 8.0, SIZE).also {
    it.setNumber(8)
  }

  public override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.stroke = BasicStroke(3f)
    g2.shear(-.1, 0.0)
    val sv = width / (help.bounds.width * 8.0)
    g2.scale(sv, sv)
    help.drawNumber(g2)
    g2.paint = Color.RED
    g2.font = font.deriveFont(32f)
    val r = help.bounds
    g2.drawString("A", r.x + r.width / 3f, r.y - r.height / 1.5f)
    g2.drawString("B", r.x + r.width / 1.5f, r.y - r.height / 3f)
    g2.drawString("C", r.x + r.width / 1.5f, r.y + r.height / 2f)
    g2.drawString("D", r.x + r.width / 3f, r.y + r.height / 1.1f)
    g2.drawString("E", r.x.toFloat(), r.y + r.height / 2f)
    g2.drawString("F", r.x.toFloat(), r.y - r.height / 3f)
    g2.drawString("G", r.x + r.width / 3f, r.y.toFloat())
    g2.dispose()
  }

  companion object {
    private const val SIZE = 16.0
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
