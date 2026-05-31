package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

fun createUI(): Component = JPanel(BorderLayout()).also {
  it.add(CubeTransitionPanel())
  it.preferredSize = Dimension(320, 240)
}

/**
 * Pseudo 3D cube transition panel. (Page transition by clicking or dragging)
 * - Click on the right half of the screen: go to the next page
 * - Click on the left half of the screen: go to the previous page
 * - Dragging with the mouse: manual interactive transition
 */
private class CubeTransitionPanel : JPanel() {
  private val screenArrX = DoubleArray(IMG_WIDTH + 1)
  private val screenArrY = DoubleArray(IMG_WIDTH + 1) // Top-left Y of each slice
  private val drawArrH = DoubleArray(IMG_WIDTH + 1) // Height of each slice

  private val images: MutableList<BufferedImage> = ArrayList()
  private var currentIndex = 0
  private var nextIndex = 1
  private var angle = 0.0 // Current rotation angle in degrees: -90..90
  private var velocity = 0.0
  private var isDragging = false
  private val pressedPt = Point()
  private var movedWhilePressed = false
  private var lastMouseX = 0

  // Offscreen Buffers
  private var faceBufferA: BufferedImage? = null
  private var faceBufferB: BufferedImage? = null
  private var finalBuffer: BufferedImage? = null
  private var handler: MouseAdapter? = null

  init {
    images.add(createSampleImage(Color.BLUE, "A"))
    images.add(createSampleImage(Color.RED, "B"))
    images.add(createSampleImage(Color.GREEN, "C"))
    images.add(createSampleImage(Color.ORANGE, "D"))

    // Animation timer targeting approximately 60 FPS (16ms interval)
    val timer = Timer(16) { updateTransition() }
    timer.start()
  }

  /**
   * Updates the transition state (angle, velocity, indexes) and schedules a repaint.
   */
  private fun updateTransition() {
    if (!isDragging) {
      angle += velocity

      // Decelerate and snap to target if velocity is low
      val isVelocitySmall = abs(velocity) < .1
      if (isVelocitySmall) {
        val v = if (angle > 0) 90.0 else -90.0
        val target = if (abs(angle) > 45) v else 0.0
        angle += (target - angle) * .2
      }

      val isOverPos = angle >= 90
      val isOverNeg = angle <= -90
      if (isOverPos) {
        currentIndex = nextIndex
        angle = 0.0
        velocity = 0.0
        nextIndex = (currentIndex + 1) % images.size
      } else if (isOverNeg) {
        currentIndex = nextIndex
        angle = 0.0
        velocity = 0.0
        nextIndex = (currentIndex - 1 + images.size) % images.size
      }
    }
    repaint()
  }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    handler = TransitionClickHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
  }

  private fun createSampleImage(color: Color, text: String): BufferedImage {
    val img = BufferedImage(
      IMG_WIDTH,
      IMG_HEIGHT,
      BufferedImage.TYPE_INT_RGB,
    )
    val g2 = img.createGraphics()
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = color
    g2.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT)
    g2.color = Color.WHITE
    g2.font = g2.font.deriveFont(Font.BOLD, 60f)
    val x = (IMG_WIDTH - g2.fontMetrics.stringWidth(text)) / 2
    val y = IMG_HEIGHT / 2
    g2.drawString(text, x, y)
    g2.dispose()
    return img
  }

  /**
   * Supports buffer initialization and resizing based on panel dimensions.
   */
  private fun ensureBuffers(w: Int, h: Int) {
    if (finalBuffer?.width != w || finalBuffer?.height != h) {
      faceBufferA = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      faceBufferB = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      finalBuffer = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val w = getWidth()
    val h = getHeight()
    ensureBuffers(w, h)

    val angCurr = angle
    val angNext = if (angle > 0) angle - 90 else angle + 90
    val firstHalf = abs(angle) < 45
    val cx = w / 2
    val cy = h / 2

    val imgCurr = images[currentIndex]
    val imgNext = images[nextIndex]

    clearBuffer(finalBuffer, Color.BLACK)
    clearBuffer(faceBufferA, null)

    // Z-sorting: Draw the back-facing side first, then the front-facing side
    if (firstHalf) {
      // Back: next side
      renderFaceToBuffer(faceBufferA!!, imgNext, angNext, cx, cy)
      // Front: current side
      clearBuffer(faceBufferB!!, null)
      renderFaceToBuffer(faceBufferB!!, imgCurr, angCurr, cx, cy)
    } else {
      // Back: current side
      renderFaceToBuffer(faceBufferA!!, imgCurr, angCurr, cx, cy)
      // Front: next side
      clearBuffer(faceBufferB!!, null)
      renderFaceToBuffer(faceBufferB!!, imgNext, angNext, cx, cy)
    }

    // Composite back buffer and front buffer into the final buffer
    compositeBuffer(finalBuffer, faceBufferA)
    compositeBuffer(finalBuffer, faceBufferB)

    // Transfer finalBuffer to screen
    g.drawImage(finalBuffer, 0, 0, null)
  }

  private fun clearBuffer(buf: BufferedImage?, color: Color?) {
    val g2 = buf?.createGraphics() ?: return
    g2.composite = AlphaComposite.Clear
    g2.fillRect(0, 0, buf.width, buf.height)
    // If color is null, clear with complete transparency (ARGB=0)
    if (color != null) {
      g2.composite = AlphaComposite.SrcOver
      g2.color = color
      g2.fillRect(0, 0, buf.width, buf.height)
    }
    g2.dispose()
  }

  private fun compositeBuffer(dst: BufferedImage?, src: BufferedImage?) {
    val g2 = dst?.createGraphics() ?: return
    g2.composite = AlphaComposite.SrcOver
    g2.drawImage(src, 0, 0, null)
    g2.dispose()
  }

  /**
   * Draws a single cube face into the offscreen buffer using perspective slicing.
   */
  private fun renderFaceToBuffer(
    buf: BufferedImage,
    img: BufferedImage,
    offsetAngle: Double,
    cx: Int,
    cy: Int,
  ) {
    // 1. Calculate perspective projections for all X positions
    calculateProjection(offsetAngle, cx, cy)

    // Find the horizontal boundaries for clipping
    var minScreenX = Double.MAX_VALUE
    var maxScreenX = -Double.MAX_VALUE
    for (sx in screenArrX) {
      minScreenX = min(minScreenX, sx)
      maxScreenX = max(maxScreenX, sx)
    }

    val clipX = max(0, floor(minScreenX).toInt())
    val clipW = min(buf.width, ceil(maxScreenX).toInt()) - clipX
    if (clipW <= 0) {
      return
    }

    // 2. Render each vertical slice onto the buffer
    val g2 = buf.createGraphics()
    g2.setClip(clipX, 0, clipW, buf.height)

    val rad = Math.toRadians(offsetAngle)
    val sin = sin(rad)
    val cos = cos(rad)
    val radius = IMG_WIDTH / 2.0

    val sliceRect = Rectangle()
    @Suppress("LoopWithTooManyJumpStatements")
    for (x in 0..<IMG_WIDTH) {
      val sx = screenArrX[x].roundToInt()
      val sxNext = screenArrX[x + 1].roundToInt()
      val sliceW = sxNext - sx
      if (sliceW <= 0) {
        continue // Skip back-facing or reversed slices
      }

      val sy = screenArrY[x].roundToInt()
      val drawH = drawArrH[x].roundToInt()
      if (drawH <= 0) {
        continue
      }
      // Draws a single vertical slice of the image texture.
      g2.drawImage(
        img,
        sx,
        sy,
        sx + sliceW,
        sy + drawH,
        x,
        0,
        x + 1,
        IMG_HEIGHT,
        null,
      )

      sliceRect.setBounds(sx, sy, sliceW, drawH)
      applyShadingSlice(g2, sliceRect, radius, sin, cos, x)
    }
    g2.dispose()
  }

  /**
   * Projects 3D coordinates onto a 2D viewport for each vertical line of the image.
   */
  private fun calculateProjection(offsetAngle: Double, cx: Int, cy: Int) {
    val rad = Math.toRadians(offsetAngle)
    val cos = cos(rad)
    val sin = sin(rad)
    val radius = IMG_WIDTH / 2.0

    for (x in 0..IMG_WIDTH) {
      val localX = x - radius
      val localZ = -radius

      // Rotate coordinates around Y-axis
      val rx = localX * cos - localZ * sin
      val rz = localX * sin + localZ * cos

      // Calculate perspective scale factor
      val scale = PERSPECTIVE / (PERSPECTIVE + rz)
      screenArrX[x] = cx + rx * scale
      screenArrY[x] = cy - IMG_HEIGHT / 2.0 * scale
      drawArrH[x] = IMG_HEIGHT * scale
    }
  }

  /**
   * Applies depth shading to a specific vertical slice to enhance the 3D effect.
   */
  @Suppress("LongParameterList")
  fun applyShadingSlice(
    g2: Graphics2D,
    rect: Rectangle,
    radius: Double,
    sin: Double,
    cos: Double,
    x: Int,
  ) {
    val localX = x - radius
    val localZ = -radius
    val rz = localX * sin + localZ * cos

    // Normalize shade value into a 0.0 - 1.0 range based on depth (Z)
    var shade = (rz + radius) / (radius * 2.0)
    // shade = min(max(shade, 0.0), 1.0)
    shade = shade.coerceIn(0.0, 1.0)

    val alpha = shade.toFloat() * MAX_SHADE_ALPHA
    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
    g2.color = Color.BLACK
    g2.fill(rect)
    g2.composite = AlphaComposite.SrcOver // Restore default composite
  }

  /**
   * Mouse listener and motion listener to handle transitions via clicking and dragging.
   */
  private inner class TransitionClickHandler : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      isDragging = false
      movedWhilePressed = false
      pressedPt.location = e.getPoint()
      lastMouseX = e.getX()
      velocity = 0.0
    }

    override fun mouseDragged(e: MouseEvent) {
      val dx = e.getX() - lastMouseX
      lastMouseX = e.getX()
      val ax = abs(e.getX() - pressedPt.x)
      val ay = abs(e.getY() - pressedPt.y)
      val totalMove = ax + ay

      if (totalMove > DRAG_THRESHOLD) {
        movedWhilePressed = true
        isDragging = true
      }

      if (isDragging) {
        angle += dx * .5
        velocity = dx * .5
        val size = images.size
        // angle = min(max(angle, -90.0), 90.0)
        angle.coerceIn(-90.0..90.0)
        nextIndex = if (angle > 0) {
          (currentIndex + 1) % size
        } else {
          (currentIndex - 1 + size) % size
        }
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      isDragging = false
    }

    override fun mouseClicked(e: MouseEvent) {
      if (!movedWhilePressed) {
        val goNext = e.getX() >= getWidth() / 2
        if (goNext) {
          nextIndex = (currentIndex + 1) % images.size
          velocity = CLICK_VELOCITY
        } else {
          nextIndex = (currentIndex - 1 + images.size) % images.size
          velocity = -CLICK_VELOCITY
        }
      }
    }
  }

  companion object {
    private const val IMG_WIDTH = 240
    private const val IMG_HEIGHT = 160
    private const val PERSPECTIVE = 800.0
    private const val CLICK_VELOCITY = 8.0
    private const val DRAG_THRESHOLD = 5
    private const val MAX_SHADE_ALPHA = .5f
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
