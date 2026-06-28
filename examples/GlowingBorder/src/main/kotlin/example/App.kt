package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val animationTimer = Timer(16, null)

fun createUI(): Component {
  val label = object : JLabel("Glowing Border Animation") {
    override fun getPreferredSize() = Dimension(260, 100)

    override fun removeNotify() {
      animationTimer.stop()
      super.removeNotify()
    }
  }
  label.setForeground(Color(0xCC_CC_DD))
  label.setHorizontalAlignment(SwingConstants.CENTER)

  val glowingBorder = GlowingBorder(14, 24, 14, 24)
  label.setBorder(glowingBorder)

  animationTimer.addActionListener {
    glowingBorder.requestNextFrame()
    label.repaint()
  }
  animationTimer.start()

  return JPanel(GridBagLayout()).also {
    it.add(label)
    it.setBackground(Color(0x1E_1E_1E))
    it.preferredSize = Dimension(320, 240)
  }
}

private class GlowingBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int,
) : EmptyBorder(top, left, bottom, right) {
  private val frameBuffer = AtomicReference<BufferedImage?>(null)
  private val isWorkerBusy = AtomicBoolean(false)
  private val cachedBounds = Rectangle()
  private val scanRect = Rectangle()
  private var currentAngle = 0.0

  fun requestNextFrame() {
    currentAngle += ROTATION_SPEED * 2 * Math.PI
    if (isWorkerBusy.compareAndSet(false, true)) {
      object : SwingWorker<BufferedImage?, Void>() {
        override fun doInBackground() = if (cachedBounds.isEmpty ||
          scanRect.isEmpty
        ) {
          null
        } else {
          renderFrame(cachedBounds, scanRect, currentAngle)
        }

        override fun done() {
          runCatching {
            val img = get()
            if (img != null) {
              frameBuffer.set(img)
            }
          }.onFailure {
            Thread.currentThread().interrupt()
            Toolkit.getDefaultToolkit().beep()
          }.also {
            isWorkerBusy.set(false)
          }
        }
      }.execute()
    }
  }

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val fx = x.toFloat()
    val fy = y.toFloat()
    val fw = width - 1f
    val fh = height - 1f
    val outer = RoundRectangle2D.Float(fx, fy, fw, fh, ARC, ARC)
    val inner = RoundRectangle2D.Float(
      fx + STROKE,
      fy + STROKE,
      fw - STROKE * 2,
      fh - STROKE * 2,
      max(2f, ARC - STROKE * 2),
      max(2f, ARC - STROKE * 2),
    )
    val borderMask = Area(outer)
    borderMask.subtract(Area(inner))

    if (width != cachedBounds.width || height != cachedBounds.height) {
      val bounds = borderMask.bounds2D
      scanRect.x = max(0, floor(bounds.x).toInt() - 1)
      scanRect.y = max(0, floor(bounds.y).toInt() - 1)
      val scanX2 = min(width, ceil(bounds.maxX).toInt() + 1)
      val scanY2 = min(height, ceil(bounds.maxY).toInt() + 1)
      scanRect.width = scanX2 - scanRect.x
      scanRect.height = scanY2 - scanRect.y
      cachedBounds.width = width
      cachedBounds.height = height
      frameBuffer.set(null)
    }

    val offscreen = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2 = offscreen.createGraphics()
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.composite = AlphaComposite.Src
    g2.fill(borderMask)

    g2.composite = AlphaComposite.SrcAtop
    g2.color = Color(0x25_25_26)
    g2.fill(borderMask)

    g2.color = Color(0x3C_3C_3C)
    g2.draw(outer)
    g2.draw(inner)

    val frame = frameBuffer.get()
    if (frame != null) {
      g2.drawImage(frame, x, y, null)
    }
    g2.dispose()

    g.drawImage(offscreen, 0, 0, null)
  }

  companion object {
    private const val STROKE = 1f
    private const val ARC = 16f
    private const val ORIGIN_X = .50f
    private const val ORIGIN_Y = .50f
    private const val ROTATION_SPEED = 1.0 / 120.0
    private val FRACTIONS = floatArrayOf(0f, .04f, .13f, .22f, 1f)
    private val COLORS = arrayOf(
      Color(0x00_00_7A_CC, true),
      Color(0xBF_00_7A_CC.toInt(), true),
      Color(0xFF_4F_C1_FF.toInt(), true),
      Color(0xBF_00_5F_B8.toInt(), true),
      Color(0x00_00_5F_B8, true),
    )

    private fun renderFrame(
      f: Rectangle,
      r: Rectangle,
      startAngle: Double,
    ): BufferedImage {
      val fw = f.width
      val fh = f.height
      val img = BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB)
      val layer = BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB)
      val cx = (ORIGIN_X * fw).toInt()
      val cy = (ORIGIN_Y * fh).toInt()
      for (py in r.y..<r.y + r.height) {
        for (px in r.x..<r.x + r.width) {
          val angle = atan2((py - cy).toDouble(), (px - cx).toDouble()) - startAngle
          val v = angle / (2 * Math.PI)
          val t = (v % 1.0 + 1.0) % 1.0
          val layerArgb = interpolateColorRgb(t)
          if ((layerArgb ushr 24) == 0) {
            continue
          }
          layer.setRGB(px, py, layerArgb)
        }
      }

      val g2 = img.createGraphics()
      try {
        g2.composite = AlphaComposite.SrcOver
        g2.drawImage(layer, 0, 0, null)
      } finally {
        g2.dispose()
      }
      return img
    }

    @Suppress("ReturnCount")
    private fun interpolateColorRgb(t: Double): Int {
      if (t <= FRACTIONS[0]) {
        return 0
      }
      val last = FRACTIONS.size - 1
      if (t >= FRACTIONS[last]) {
        return COLORS[last].rgb
      }
      for (i in 0..<last) {
        if (t <= FRACTIONS[i + 1]) {
          val r = ((t - FRACTIONS[i]) / (FRACTIONS[i + 1] - FRACTIONS[i]))
          return interpolateArgb(COLORS[i].rgb, COLORS[i + 1].rgb, r.toFloat())
        }
      }
      return 0
    }

    private fun interpolateArgb(c0: Int, c1: Int, t: Float): Int {
      val a = lerp((c0 ushr 24) and 0xFF, (c1 ushr 24) and 0xFF, t)
      val r = lerp((c0 ushr 16) and 0xFF, (c1 ushr 16) and 0xFF, t)
      val g = lerp((c0 ushr 8) and 0xFF, (c1 ushr 8) and 0xFF, t)
      val b = lerp(c0 and 0xFF, c1 and 0xFF, t)
      return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).roundToInt()
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
