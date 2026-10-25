package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import java.awt.image.MemoryImageSource
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.abs
import kotlin.math.roundToInt

fun createUI(): Component {
  val canvas = PixelCanvas(128, 128)

  // Center the canvas in the viewport when it is smaller than the viewport
  val wrapper = JPanel(GridBagLayout())
  wrapper.add(canvas)
  val scroll = JScrollPane(wrapper)
  scroll.verticalScrollBar.unitIncrement = 16
  scroll.horizontalScrollBar.unitIncrement = 16

  return JPanel(BorderLayout()).also {
    it.add(createToolBox(canvas), BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBox(canvas: PixelCanvas): Component {
  val zoomModel = SpinnerNumberModel(
    canvas.zoom,
    PixelCanvas.MIN_ZOOM,
    PixelCanvas.MAX_ZOOM,
    1,
  )
  zoomModel.addChangeListener { canvas.setZoom(zoomModel.number.toInt(), null) }
  canvas.addPropertyChangeListener(PixelCanvas.ZOOM) { zoomModel.value = it.newValue }

  val penModel = SpinnerNumberModel(canvas.penSize, 1, 16, 1)
  penModel.addChangeListener { canvas.penSize = penModel.number.toInt() }

  val grid = JCheckBox("Grid", canvas.isGridVisible)
  grid.addActionListener { canvas.isGridVisible = grid.isSelected }

  val icon = ColorIcon(canvas.penColor)
  val color = JButton("Color", icon)
  color.addActionListener {
    val c = JColorChooser.showDialog(color.rootPane, "Pen color", icon.color)
    if (c != null) {
      icon.color = c
      canvas.penColor = c
      color.repaint()
    }
  }

  val clear = JButton("Clear")
  clear.addActionListener { canvas.clear() }

  val p1 = JPanel(FlowLayout(FlowLayout.LEADING, 2, 2))
  p1.add(JLabel("Zoom:"))
  p1.add(JSpinner(zoomModel))
  p1.add(JLabel("Pen size:"))
  p1.add(JSpinner(penModel))
  addFileButtons(p1, canvas)

  val p2 = JPanel(FlowLayout(FlowLayout.LEADING, 2, 2))
  p2.add(grid)
  p2.add(color)
  p2.add(clear)

  return JPanel(GridLayout(2, 1)).also {
    it.add(p1)
    it.add(p2)
  }
}

// The Save and Load buttons share a JFileChooser to remember the last directory
private fun addFileButtons(
  p: JPanel,
  canvas: PixelCanvas,
) {
  val chooser = JFileChooser()
  chooser.fileFilter = FileNameExtensionFilter("PNG image (*.png)", "png")
  val save = JButton("Save")
  save.addActionListener { saveImage(chooser, canvas) }
  val load = JButton("Load")
  load.addActionListener { loadImage(chooser, canvas) }
  p.add(save)
  p.add(load)
}

private fun saveImage(
  chooser: JFileChooser,
  canvas: PixelCanvas,
) {
  val parent = canvas.rootPane
  if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
    val file = withPngExtension(chooser.selectedFile)
    if (!file.exists() || confirmReplace(parent, file)) {
      try {
        ImageIO.write(canvas.toBufferedImage(), "png", file)
      } catch (ex: IOException) {
        showErrorMessage(parent, ex.message)
      }
    }
  }
}

private fun withPngExtension(file: File): File {
  val name = file.name
  val isPng = name.lowercase(Locale.ENGLISH).endsWith(".png")
  return if (isPng) file else File(file.parentFile, "$name.png")
}

private fun confirmReplace(
  parent: Component,
  file: File,
): Boolean {
  val msg = "%s already exists.%nDo you want to replace it?".format(file.name)
  val answer = JOptionPane.showConfirmDialog(
    parent,
    msg,
    "Save",
    JOptionPane.YES_NO_OPTION,
    JOptionPane.WARNING_MESSAGE,
  )
  return answer == JOptionPane.YES_OPTION
}

private fun loadImage(
  chooser: JFileChooser,
  canvas: PixelCanvas,
) {
  val parent = canvas.rootPane
  if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
    val file = chooser.selectedFile
    try {
      // ImageIO#read(...) returns null if no registered ImageReader can read the file
      val img = ImageIO.read(file)
      if (img == null) {
        showErrorMessage(parent, "Unsupported image format: " + file.name)
      } else {
        canvas.setImage(img)
      }
    } catch (ex: IOException) {
      showErrorMessage(parent, ex.message)
    }
  }
}

private fun showErrorMessage(
  parent: Component,
  msg: String?,
) {
  UIManager.getLookAndFeel().provideErrorFeedback(parent)
  JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE)
}

private class PixelCanvas(
  private val imageWidth: Int,
  private val imageHeight: Int,
) : JPanel() {
  private val imageBounds = Rectangle(imageWidth, imageHeight)
  private val pixels = IntArray(imageWidth * imageHeight)
  private val source = MemoryImageSource(
    imageWidth,
    imageHeight,
    pixels,
    0,
    imageWidth,
  ).also {
    // Reuse a single Image and send only the changed area with newPixels(...)
    it.setAnimated(true)
  }
  private val image = Toolkit.getDefaultToolkit().createImage(source)

  // The image pixel under the mouse cursor, or null if the cursor is outside
  private var cursorPixel: Point? = null
  private var handler: MouseAdapter? = null
  private var penArgb = 0xFF_00_00_00.toInt()

  var zoom = 2
    private set

  var penSize = 1
    set(size) {
      val pt = cursorPixel
      setCursorPixel(null)
      field = maxOf(1, size)
      setCursorPixel(pt)
    }

  var penColor: Color
    get() = Color(penArgb, true)
    set(color) {
      penArgb = color.rgb
    }

  var isGridVisible = true
    set(visible) {
      field = visible
      repaint()
    }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    removeMouseWheelListener(handler)
    super.updateUI()
    handler = CanvasHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    addMouseWheelListener(handler)
  }

  override fun getPreferredSize() = Dimension(imageWidth * zoom, imageHeight * zoom)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = imageWidth * zoom
    val h = imageHeight * zoom
    // The opaque checkerboard background shows the transparent pixels
    g2.paint = TEXTURE
    g2.fillRect(0, 0, w, h)
    // Enlarge each image pixel into a zoom x zoom square without blurring
    g2.setRenderingHint(
      RenderingHints.KEY_INTERPOLATION,
      RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
    )
    g2.drawImage(image, 0, 0, w, h, this)
    if (isGridVisible && zoom >= GRID_MIN_ZOOM) {
      paintGrid(g2, g2.clipBounds)
    }
    cursorPixel?.also {
      val r = toViewRect(getPenRect(it.x, it.y))
      g2.paint = Color.BLACK
      g2.drawRect(r.x, r.y, r.width - 1, r.height - 1)
      g2.paint = Color.WHITE
      g2.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3)
    }
    g2.dispose()
  }

  private fun paintGrid(
    g2: Graphics2D,
    clip: Rectangle?,
  ) {
    val r = clip ?: Rectangle(preferredSize)
    val w = imageWidth * zoom
    val h = imageHeight * zoom
    val x0 = maxOf(0, r.x / zoom)
    val x1 = minOf(imageWidth, (r.x + r.width) / zoom + 1)
    val y0 = maxOf(0, r.y / zoom)
    val y1 = minOf(imageHeight, (r.y + r.height) / zoom + 1)
    g2.paint = GRID_COLOR
    for (x in x0..x1) {
      g2.drawLine(x * zoom, 0, x * zoom, h)
    }
    for (y in y0..y1) {
      g2.drawLine(0, y * zoom, w, y * zoom)
    }
  }

  // anchor: the point in this component's coordinates that should stay
  // under the mouse cursor, or null to keep the center of the viewport
  fun setZoom(
    newZoom: Int,
    anchor: Point?,
  ) {
    val oldZoom = zoom
    val z = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    if (z != oldZoom) {
      zoom = z
      firePropertyChange(ZOOM, oldZoom, z)
      val viewport = SwingUtilities.getAncestorOfClass(JViewport::class.java, this)
      if (viewport is JViewport) {
        keepAnchor(viewport, anchor, z.toDouble() / oldZoom)
      } else {
        revalidate()
      }
      repaint()
    }
  }

  // Scrolls the viewport so that the anchor point stays at the same location
  // in the viewport after the zoom factor has been multiplied by scale
  private fun keepAnchor(
    viewport: JViewport,
    anchor: Point?,
    scale: Double,
  ) {
    // If the anchor is null, use the center of the visible area.
    // The layout has not been updated yet, so this is the old visible area
    val pt = anchor ?: visibleRect.let {
      Point(it.centerX.toInt(), it.centerY.toInt())
    }
    val vpt = SwingUtilities.convertPoint(this, pt, viewport)
    // Layout immediately so that the new view size is available
    invalidate()
    viewport.parent.validate()
    val ax = (pt.x * scale).roundToInt()
    val ay = (pt.y * scale).roundToInt()
    val p = SwingUtilities.convertPoint(this, ax, ay, viewport.view)
    val extent = viewport.extentSize
    val view = viewport.viewSize
    val x = maxOf(0, minOf(p.x - vpt.x, view.width - extent.width))
    val y = maxOf(0, minOf(p.y - vpt.y, view.height - extent.height))
    viewport.viewPosition = Point(x, y)
  }

  fun clear() {
    pixels.fill(0x0)
    source.newPixels()
    repaint()
  }

  // Copies the pixels into a new ARGB image, keeping the transparent pixels
  fun toBufferedImage(): BufferedImage {
    val img = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
    img.setRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth)
    return img
  }

  // Copies the pixels of the image into the canvas, whose size is fixed.
  // BufferedImage#getRGB(...) converts any image type (indexed, gray, RGB)
  // to ARGB, and the area outside the canvas is clipped
  fun setImage(img: BufferedImage) {
    pixels.fill(0x0)
    val w = minOf(imageWidth, img.width)
    val h = minOf(imageHeight, img.height)
    img.getRGB(0, 0, w, h, pixels, 0, imageWidth)
    source.newPixels()
    repaint()
  }

  // Converts a point in this component's coordinates to an image pixel position
  private fun toImagePoint(pt: Point) =
    Point(Math.floorDiv(pt.x, zoom), Math.floorDiv(pt.y, zoom))

  private fun toViewRect(r: Rectangle) =
    Rectangle(r.x * zoom, r.y * zoom, r.width * zoom, r.height * zoom)

  // The pen tip always covers penSize x penSize image pixels,
  // regardless of the zoom factor
  private fun getPenRect(
    px: Int,
    py: Int,
  ): Rectangle {
    val offset = (penSize - 1) / 2
    return Rectangle(px - offset, py - offset, penSize, penSize)
  }

  private fun setCursorPixel(pt: Point?) {
    cursorPixel?.also {
      repaint(toViewRect(getPenRect(it.x, it.y)))
    }
    cursorPixel = pt
    if (pt != null) {
      repaint(toViewRect(getPenRect(pt.x, pt.y)))
    }
  }

  // Draws a line between two image pixels with the Bresenham algorithm
  private fun drawLine(
    p0: Point,
    p1: Point,
    color: Int,
  ) {
    val dx = abs(p1.x - p0.x)
    val dy = -abs(p1.y - p0.y)
    val sx = if (p0.x < p1.x) 1 else -1
    val sy = if (p0.y < p1.y) 1 else -1
    var err = dx + dy
    var x = p0.x
    var y = p0.y
    stamp(x, y, color)
    while (x != p1.x || y != p1.y) {
      val e2 = 2 * err
      if (e2 >= dy) {
        err += dy
        x += sx
      }
      if (e2 <= dx) {
        err += dx
        y += sy
      }
      stamp(x, y, color)
    }
    // Every point of the line lies within the bounding box of both end points,
    // so the changed area is the union of the pen tips at both ends
    val dirty = getPenRect(p0.x, p0.y)
      .union(getPenRect(p1.x, p1.y))
      .intersection(imageBounds)
    if (!dirty.isEmpty) {
      source.newPixels(dirty.x, dirty.y, dirty.width, dirty.height)
      repaint(toViewRect(dirty))
    }
  }

  private fun stamp(
    px: Int,
    py: Int,
    color: Int,
  ) {
    val r = getPenRect(px, py).intersection(imageBounds)
    // When the pen tip is outside the image, the intersection may have
    // a negative width, and IntArray#fill(...) would throw an exception
    if (r.isEmpty) {
      return
    }
    for (y in r.y..<r.y + r.height) {
      val from = y * imageWidth + r.x
      pixels.fill(color, from, from + r.width)
    }
  }

  private inner class CanvasHandler : MouseAdapter() {
    private val prev = Point()
    private var color = 0
    private var drawing = false

    override fun mousePressed(e: MouseEvent) {
      color = when {
        SwingUtilities.isLeftMouseButton(e) -> penArgb
        SwingUtilities.isRightMouseButton(e) -> 0x0 // eraser
        else -> return
      }
      drawing = true
      val pt = toImagePoint(e.point)
      prev.location = pt
      drawLine(pt, pt, color)
    }

    override fun mouseDragged(e: MouseEvent) {
      val pt = toImagePoint(e.point)
      setCursorPixel(pt)
      if (drawing && pt != prev) {
        drawLine(prev, pt, color)
        prev.location = pt
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      drawing = false
    }

    override fun mouseMoved(e: MouseEvent) {
      setCursorPixel(toImagePoint(e.point))
    }

    override fun mouseExited(e: MouseEvent) {
      setCursorPixel(null)
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      if (e.isControlDown) {
        setCursorPixel(null)
        setZoom(zoom - e.wheelRotation, e.point)
        // The mouse cursor now points to a different location in this component
        val pt = e.locationOnScreen
        SwingUtilities.convertPointFromScreen(pt, this@PixelCanvas)
        setCursorPixel(toImagePoint(pt))
      } else {
        // Forward to the JScrollPane to scroll normally
        val p = parent
        p.dispatchEvent(SwingUtilities.convertMouseEvent(e.component, e, p))
      }
    }
  }

  companion object {
    const val ZOOM = "zoom"
    const val MIN_ZOOM = 1
    const val MAX_ZOOM = 32
    private const val GRID_MIN_ZOOM = 4
    private val GRID_COLOR = Color(0xA0_64_95_ED.toInt(), true)
    private val TEXTURE = createCheckerTexture(8, Color.WHITE, Color(0xE8_E8_E8))

    fun createCheckerTexture(
      cellSize: Int,
      background: Color,
      color: Color,
    ): TexturePaint {
      val size = cellSize * 2
      val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics()
      g2.paint = background
      g2.fillRect(0, 0, size, size)
      g2.paint = color
      g2.fillRect(0, 0, cellSize, cellSize)
      g2.fillRect(cellSize, cellSize, cellSize, cellSize)
      g2.dispose()
      return TexturePaint(img, Rectangle(size, size))
    }
  }
}

private class ColorIcon(
  var color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.paint = Color.GRAY
    g2.drawRect(0, 0, iconWidth - 1, iconHeight - 1)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
