package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val button1 = RadialGradientButton("JButton JButton JButton JButton")
  button1.foreground = Color.WHITE

  val button2 = RadialGradientPaintButton("JButton JButton JButton JButton")
  button2.foreground = Color.WHITE

  val p = object : JPanel(FlowLayout(FlowLayout.CENTER, 20, 50)) {
    // private val texture = TextureUtils.createCheckerTexture(16, Color(-0x11cdcdce, true))
    // private val texture = TextureUtils.createCheckerTexture(16, Color(0xEE_32_32_32.toInt(), true))
    private val texture = TextureUtils.createCheckerTexture(16, Color(0x32, 0x32, 0x32, 0xEE))

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  p.isOpaque = false
  p.add(button1)
  p.add(button2)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RadialGradientButton(title: String) : JButton(title) {
  private val timer1 = Timer(10, null)
  private val timer2 = Timer(10, null)
  private val pt = Point()
  private var radius = 0f
  private var shape: Shape? = null
  private var base: Rectangle? = null

  init {
    timer1.addActionListener {
      radius = minOf(200f, radius + DELTA)
      repaint()
    }
    timer2.addActionListener {
      radius = maxOf(0f, radius - DELTA)
      repaint()
    }
    val listener = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) {
        timer2.stop()
        if (!timer1.isRunning) {
          timer1.start()
        }
      }

      override fun mouseExited(e: MouseEvent) {
        timer1.stop()
        if (!timer2.isRunning) {
          timer2.start()
        }
      }

      override fun mouseMoved(e: MouseEvent) {
        pt.location = e.point
        e.component.repaint()
      }

      override fun mouseDragged(e: MouseEvent) {
        mouseMoved(e)
        // pt.location = e.point
        // repaint()
      }
    }
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    isContentAreaFilled = false
    isFocusPainted = false
    background = Color(0xF7_23_59)
    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    update()
  }

  private fun update() {
    if (bounds != base) {
      base = bounds
      shape = RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, ARC_WIDTH, ARC_HEIGHT)
    }
  }

  override fun contains(x: Int, y: Int): Boolean {
    update()
    return shape?.contains(Point(x, y)) ?: false
  }

  // override fun paintBorder(g: Graphics) {
  //   update()
  //   val g2 = g.create() as? Graphics2D ?: return
  //   g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  //   // g2.setStroke(BasicStroke(2.5f))
  //   if (getModel().isArmed()) {
  //     g2.paint = Color(0x64_44_05_F7, true)
  //   } else {
  //     g2.paint = Color(0xF7_23_59).darker()
  //   }
  //   g2.draw(shape)
  //   g2.dispose()
  // }


  override fun paintComponent(g: Graphics) {
    update()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    // g2.setComposite(AlphaComposite.Clear)
    // g2.setPaint(Color(0x0, true))
    // g2.fillRect(0, 0, getWidth(), getHeight())

    g2.composite = AlphaComposite.Src
    g2.paint = Color(if (getModel().isArmed) 0xFF_AA_AA else 0xF7_23_59)
    g2.fill(shape)

    if (radius > 0) {
      val r2 = radius + radius
      // Stunning hover effects with CSS variables ? Prototypr
      // https://blog.prototypr.io/stunning-hover-effects-with-css-variables-f855e7b95330
      val colors = arrayOf(Color(0x64_44_05_F7, true), Color(0x00_F7_23_59, true))
      g2.paint = RadialGradientPaint(pt, r2, floatArrayOf(0f, 1f), colors)
      g2.composite = AlphaComposite.SrcAtop
      g2.clip = shape
      g2.fill(Ellipse2D.Float(pt.x - radius, pt.y - radius, r2, r2))
    }
    g2.dispose()

    super.paintComponent(g)
  }

  companion object {
    private const val DELTA = 10f
    private const val ARC_WIDTH = 32f
    private const val ARC_HEIGHT = 32f
  }
}

private class RadialGradientPaintButton(title: String) : JButton(title) {
  private val timer1 = Timer(10, null)
  private val timer2 = Timer(10, null)
  private val pt = Point()
  private var radius = 0f
  private var shape: Shape? = null
  private var base: Rectangle? = null

  @Transient
  private var buf: BufferedImage? = null

  init {
    timer1.addActionListener {
      radius = minOf(200f, radius + DELTA)
      repaint()
    }
    timer2.addActionListener {
      radius = maxOf(0f, radius - DELTA)
      repaint()
    }
    val listener = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) {
        timer2.stop()
        if (!timer1.isRunning) {
          timer1.start()
        }
      }

      override fun mouseExited(e: MouseEvent) {
        timer1.stop()
        if (!timer2.isRunning) {
          timer2.start()
        }
      }

      override fun mouseMoved(e: MouseEvent) {
        pt.location = e.point
        e.component.repaint()
      }

      override fun mouseDragged(e: MouseEvent) {
        mouseMoved(e)
        // pt.location = e.point
        // repaint()
      }
    }
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    isContentAreaFilled = false
    isFocusPainted = false
    background = Color(0xF7_23_59)
    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    update()
  }

  private fun update() {
    if (bounds != base) {
      base = bounds
      if (width > 0 && height > 0) {
        buf = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      }
      shape = RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, ARC_WIDTH, ARC_HEIGHT)
    }
    // if (buf == null) {
    //   return
    // }
    val g2 = buf?.createGraphics() ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    // val c1 = Color(0x00_F7_23_59, true)
    // val c2 = Color(0x64_44_05_F7, true)

    g2.composite = AlphaComposite.Clear
    g2.fillRect(0, 0, width, height)

    g2.composite = AlphaComposite.Src
    g2.paint = Color(if (getModel().isArmed) 0xFF_AA_AA else 0xF7_23_59)
    g2.fill(shape)

    if (radius > 0) {
      val r2 = radius + radius
      // val colors = arrayOf(c2, c1)
      val colors = arrayOf(Color(0x64_44_05_F7, true), Color(0x00_F7_23_59, true))
      g2.paint = RadialGradientPaint(pt, r2, floatArrayOf(0f, 1f), colors)
      g2.composite = AlphaComposite.SrcAtop
      // g2.setClip(shape)
      g2.fill(Ellipse2D.Float(pt.x - radius, pt.y - radius, r2, r2))
    }
    g2.dispose()
  }

  override fun contains(x: Int, y: Int): Boolean {
    update()
    return shape?.contains(Point(x, y)) ?: false
  }

  override fun paintComponent(g: Graphics) {
    update()
    g.drawImage(buf, 0, 0, this)
    super.paintComponent(g)
  }

  companion object {
    private const val DELTA = 10f
    private const val ARC_WIDTH = 32f
    private const val ARC_HEIGHT = 32f
  }
}

private object TextureUtils {
  fun createCheckerTexture(cs: Int, color: Color): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.paint = color
    g2.fillRect(0, 0, size, size)
    var i = 0
    while (i * cs < size) {
      var j = 0
      while (j * cs < size) {
        if ((i + j) % 2 == 0) {
          g2.fillRect(i * cs, j * cs, cs, cs)
        }
        j++
      }
      i++
    }
    g2.dispose()
    return TexturePaint(img, Rectangle(size, size))
  }
} /* HideUtilityClassConstructor */

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
