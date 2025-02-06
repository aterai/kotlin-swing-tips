package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import javax.swing.*
import javax.swing.plaf.metal.MetalSliderUI

private val TEXTURE = TextureUtils.createCheckerTexture(6, Color(200, 150, 100, 50))

fun makeUI(): Component {
  val icon = object : Icon {
    override fun paintIcon(
      c: Component,
      g: Graphics,
      x: Int,
      y: Int,
    ) {
      // Empty icon
    }

    override fun getIconWidth() = 15

    override fun getIconHeight() = 64
  }
  UIManager.put("Slider.horizontalThumbIcon", icon)
  // println(UIManager.get("Slider.trackWidth"))
  // println(UIManager.get("Slider.majorTickLength"))
  // println(UIManager.getInt("Slider.trackWidth"))
  // println(UIManager.getInt("Slider.majorTickLength"))
  UIManager.put("Slider.trackWidth", 64)
  UIManager.put("Slider.majorTickLength", 6)

  val slider0 = makeSlider()
  val slider1 = makeSlider()
  slider1.setUI(GradientPalletSliderUI())
  slider1.model = slider0.model

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default:", slider0))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Gradient translucent track JSlider:", slider1))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val p = object : JPanel(BorderLayout()) {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = TEXTURE
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  p.add(box, BorderLayout.NORTH)
  p.isOpaque = false
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeSlider() = object : JSlider(SwingConstants.HORIZONTAL, 0, 100, 50) {
  private var handler: MouseAdapter? = null

  override fun updateUI() {
    super.updateUI()
    handler = object : MouseAdapter() {
      override fun mouseDragged(e: MouseEvent) {
        e.component.repaint()
      }

      override fun mouseWheelMoved(e: MouseWheelEvent) {
        (e.component as? JSlider)?.model?.also {
          it.value -= e.wheelRotation
        }
      }
    }
    addMouseMotionListener(handler)
    addMouseWheelListener(handler)
    background = Color.GRAY
    isOpaque = false
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.isOpaque = false
  it.add(c)
}

private class GradientPalletSliderUI : MetalSliderUI() {
  private var controlDarkShadow = Color(0x64_64_64)
  private var controlHighlight = Color(0xC8_FF_C8)
  private var controlShadow = Color(0x00_64_00)

  override fun paintTrack(g: Graphics) {
    g.translate(trackRect.x, trackRect.y)

    var trackLeft = 0
    var trackTop = 0
    val trackRight: Int
    val trackBottom: Int
    if (slider.orientation == SwingConstants.HORIZONTAL) {
      trackBottom = trackRect.height - 1 - thumbOverhang
      trackTop = trackBottom - trackWidth + 1
      trackRight = trackRect.width - 1
    } else {
      // if (leftToRight) {
      trackLeft = trackRect.width - thumbOverhang - trackWidth
      trackRight = trackRect.width - thumbOverhang - 1
      // } else {
      //   trackLeft = getThumbOverhang()
      //   trackRight = getThumbOverhang() + getTrackWidth() - 1
      // }
      trackBottom = trackRect.height - 1
    }

    // Draw the track
    paintTrackBase(g, trackTop, trackLeft, trackBottom, trackRight)

    // Draw the fill
    paintTrackFill(g, trackTop, trackLeft, trackBottom, trackRight)

    // Draw the highlight
    paintTrackHighlight(g, trackTop, trackLeft, trackBottom, trackRight)

    g.translate(-trackRect.x, -trackRect.y)
  }

  private fun paintTrackBase(
    g: Graphics,
    trackTop: Int,
    trackLeft: Int,
    trackBottom: Int,
    trackRight: Int,
  ) {
    if (slider.isEnabled) {
      g.color = controlDarkShadow
      g.drawRect(
        trackLeft,
        trackTop,
        trackRight - trackLeft - 1,
        trackBottom - trackTop - 1,
      )

      g.color = controlHighlight
      g.drawLine(trackLeft + 1, trackBottom, trackRight, trackBottom)
      g.drawLine(trackRight, trackTop + 1, trackRight, trackBottom)

      g.color = controlShadow
      g.drawLine(trackLeft + 1, trackTop + 1, trackRight - 2, trackTop + 1)
      g.drawLine(trackLeft + 1, trackTop + 1, trackLeft + 1, trackBottom - 2)
    } else {
      g.color = controlShadow
      g.drawRect(
        trackLeft,
        trackTop,
        trackRight - trackLeft - 1,
        trackBottom - trackTop - 1,
      )
    }
  }

  private fun paintTrackFill(
    g: Graphics,
    trackTop: Int,
    trackLeft: Int,
    trackBottom: Int,
    trackRight: Int,
  ) {
    var middleOfThumb: Int
    val fillTop: Int
    val fillLeft: Int
    val fillBottom: Int
    val fillRight: Int

    if (slider.orientation == SwingConstants.HORIZONTAL) {
      middleOfThumb = thumbRect.x + thumbRect.width / 2
      middleOfThumb -= trackRect.x // To compensate for the g.translate()
      fillTop = trackTop + 1
      fillBottom = trackBottom - 2
      fillLeft = trackLeft + 1
      fillRight = middleOfThumb - 2
    } else {
      middleOfThumb = thumbRect.y + thumbRect.height / 2
      middleOfThumb -= trackRect.y // To compensate for the g.translate()
      fillLeft = trackLeft
      fillRight = trackRight - 1
      fillTop = middleOfThumb
      fillBottom = trackBottom - 1
    }

    if (slider.isEnabled) {
      val x = (fillRight - fillLeft) / (trackRight - trackLeft).toFloat()
      g.color = GradientPalletUtils.getColorFromPallet(GRADIENT_PALLET, x, 0x64 shl 24)
      g.fillRect(fillLeft + 1, fillTop + 1, fillRight - fillLeft, fillBottom - fillTop)
    } else {
      g.color = controlShadow
      g.fillRect(fillLeft, fillTop, fillRight - fillLeft, trackBottom - trackTop)
    }
  }

  private fun paintTrackHighlight(
    g: Graphics,
    trackTop: Int,
    trackLeft: Int,
    trackBottom: Int,
    trackRight: Int,
  ) {
    var yy = trackTop + (trackBottom - trackTop) / 2
    for (i in 10 downTo 0) {
      g.color = makeColor(i * .07f)
      g.drawLine(trackLeft + 2, yy, trackRight - trackLeft - 2, yy)
      yy--
    }
  }

  private fun makeColor(alpha: Float) = Color(1f, 1f, 1f, alpha)

  companion object {
    private val GRADIENT_PALLET = GradientPalletUtils.makeGradientPallet()
  }
}

private object GradientPalletUtils {
  fun makeGradientPallet(): IntArray {
    val image = BufferedImage(100, 1, BufferedImage.TYPE_INT_RGB)
    val g2 = image.createGraphics()
    val start = Point2D.Float()
    val end = Point2D.Float(99f, 0f)
    val dist = floatArrayOf(.0f, .5f, 1f)
    val colors = arrayOf(Color.RED, Color.YELLOW, Color.GREEN)
    g2.paint = LinearGradientPaint(start, end, dist, colors)
    g2.fillRect(0, 0, 100, 1)
    g2.dispose()

    val width = image.getWidth(null)
    val pallet = IntArray(width)
    runCatching {
      PixelGrabber(image, 0, 0, width, 1, pallet, 0, width).grabPixels()
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
    }
    return pallet
  }

  fun getColorFromPallet(
    pallet: IntArray,
    x: Float,
    alpha: Int,
  ): Color {
    // val i = (pallet.size * x).toInt()
    // val max = pallet.size - 1
    // val index = minOf(maxOf(i, 0), max)
    val index = (pallet.size * x).toInt().coerceIn(0, pallet.size - 1)
    val pix = pallet[index] and 0x00_FF_FF_FF
    // val alpha = 0x64 << 24
    return Color(alpha or pix, true)
  }
}

private object TextureUtils {
  fun createCheckerTexture(
    cs: Int,
    color: Color,
  ): TexturePaint {
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
