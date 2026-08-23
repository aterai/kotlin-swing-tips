package example

import java.awt.*
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ColorModel
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val MIN_RADIUS = 2
private const val MAX_RADIUS = 40
private const val DEFAULT_RADIUS = 16

fun createUI(): Component {
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  split.setContinuousLayout(true)
  split.setBorder(BorderFactory.createEmptyBorder())
  split.setResizeWeight(.5)

  val cl = Thread.currentThread().contextClassLoader
  val image = cl
    .getResource("example/test.jpg")
    ?.openStream()
    ?.use(ImageIO::read)
    ?: createMissingImage()
  val imageIcon = ImageIcon(image)

  val beforeCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics?) {
      super.paintComponent(g)
      imageIcon.paintIcon(this, g, 0, 0)
    }
  }
  split.setLeftComponent(beforeCanvas)

  val afterCanvas = FilteredCanvas(image)
  afterCanvas.setRadius(DEFAULT_RADIUS)
  split.setRightComponent(afterCanvas)

  val slider = JSlider(MIN_RADIUS, MAX_RADIUS, DEFAULT_RADIUS)
  slider.addChangeListener { e ->
    val src = e.source
    if (src is JSlider) {
      afterCanvas.setRadius(src.value)
    }
  }

  val p = JPanel(BorderLayout(5, 5))
  p.add(JLabel("Radius:"), BorderLayout.WEST)
  p.add(slider)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(split)
    it.add(p, BorderLayout.SOUTH)
    it.setOpaque(false)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMissingImage(): BufferedImage {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class FilteredCanvas(
  private val image: BufferedImage,
) : JComponent() {
  private var filtered: BufferedImage? = null

  fun setRadius(radius: Int) {
    val op = HexagonalMosaicFilter(radius)
    filtered = op.filter(image, null)
    repaint()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (filtered != null) {
      g.translate(-location.x + getParent().insets.left, 0)
      g.drawImage(filtered, 0, 0, this)
    }
  }
}

private class HexagonalMosaicFilter(
  private val radius: Int,
) : BufferedImageOp {
  override fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage {
    val width = src.width
    val height = src.height
    val img = dst ?: createCompatibleDestImage(src, null)
    require(!(img.width != width || img.height != height)) {
      "src and dst must have the same size"
    }
    val columns = getGridSize(width, radius * COLUMN_PITCH)
    val rows = getGridSize(height, radius * SQRT3)
    val colors = getCellColors(src, columns, rows)
    val line = IntArray(width)
    for (y in 0..<height) {
      for (x in 0..<width) {
        line[x] = colors[getCellIndex(x, y, columns, rows)]
      }
      img.setRGB(0, y, width, 1, line, 0, width)
    }
    return img
  }

  private fun getCellColors(src: BufferedImage, columns: Int, rows: Int): IntArray {
    val width = src.width
    val height = src.height
    val size = columns * rows
    val sumA = LongArray(size)
    val sumR = LongArray(size)
    val sumG = LongArray(size)
    val sumB = LongArray(size)
    val count = IntArray(size)
    val line = IntArray(width)
    for (y in 0..<height) {
      src.getRGB(0, y, width, 1, line, 0, width)
      for (x in 0..<width) {
        val i = getCellIndex(x, y, columns, rows)
        val argb = line[x]
        val a = ((argb ushr 24) and 0xFF).toLong()
        sumA[i] += a
        sumR[i] += ((argb ushr 16) and 0xFF) * a
        sumG[i] += ((argb ushr 8) and 0xFF) * a
        sumB[i] += (argb and 0xFF) * a
        count[i]++
      }
    }
    val colors = IntArray(size)
    for (i in 0..<size) {
      val a = sumA[i]
      if (a > 0) {
        colors[i] = (
          (a / count[i]).toInt() shl 24 or (
            (sumR[i] / a).toInt() shl 16
          ) or (
            (sumG[i] / a).toInt() shl 8
          ) or (sumB[i] / a).toInt()
        )
      }
    }
    return colors
  }

  // Look up the hexagon that contains the given pixel:
  // pixel -> axial coordinates -> cube rounding -> odd-q offset coordinates
  // https://www.redblobgames.com/grids/hexagons/
  private fun getCellIndex(x: Int, y: Int, columns: Int, rows: Int): Int {
    val q = Q_SCALE * x / radius
    val r = R_SCALE * (SQRT3 * y - x) / radius
    val s = -q - r
    var cq = q.roundToInt()
    var cr = r.roundToInt()
    val cs = s.roundToInt()
    val dq = abs(cq - q)
    val dr = abs(cr - r)
    val ds = abs(cs - s)
    if (dq > dr && dq > ds) {
      cq = -cr - cs
    } else if (dr > ds) {
      cr = -cq - cs
    }
    val col = cq + MARGIN
    val row = (cr + ((cq - (cq and 1)) shr 1)) + MARGIN
    return clamp(col, columns) + clamp(row, rows) * columns
  }

  override fun getBounds2D(src: BufferedImage) = Rectangle2D.Double(
    0.0,
    0.0,
    src.width.toDouble(),
    src.height.toDouble(),
  )

  override fun createCompatibleDestImage(
    src: BufferedImage,
    dstCm: ColorModel?,
  ) = BufferedImage(
    src.width,
    src.height,
    BufferedImage.TYPE_INT_ARGB,
  )

  override fun getPoint2D(srcPt: Point2D?, dstPt: Point2D?): Point2D {
    val pt = dstPt ?: Point2D.Double()
    pt.setLocation(srcPt)
    return pt
  }

  override fun getRenderingHints() = RenderingHints(
    mutableMapOf<RenderingHints.Key?, Any?>(),
  )

  companion object {
    private val SQRT3 = sqrt(3.0)
    private const val COLUMN_PITCH = 3.0 / 2.0
    private const val Q_SCALE = 2.0 / 3.0
    private const val R_SCALE = 1.0 / 3.0
    private const val MARGIN = 1

    private fun getGridSize(length: Int, pitch: Double) =
      ceil(length / pitch).toInt() + MARGIN * 2 + 1

    private fun clamp(value: Int, size: Int) = min(max(value, 0), size - 1)
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.translate(x, y)
    g2.fillRect(0, 0, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(gap, gap, w - gap, h - gap)
    g2.drawLine(gap, h - gap, w - gap, gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
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
