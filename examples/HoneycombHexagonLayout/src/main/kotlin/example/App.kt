package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Path2D
import javax.swing.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// n determines button count per row
private const val N = 2
private const val TOTAL_ROWS = 3

// Number of buttons in the flower(1 center + 6 around) arrangement
private const val FLOWER_SIZE = 7

// Gap between adjacent hexagon edges in pixels
// 0 = perfectly touching, positive = gap
private const val BTN_GAP = 6
private val BTN_BGC = Color(0x46_82_B4) // Steel blue
private val PANEL_BGC = Color(0x2D_2D_2D)

// Bounding box ratio of a regular hexagon: sqrt(3) / 2
private val RATIO = sqrt(3.0) / 2.0

fun createUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("PointyTopped: Rows", createRowsPanel())
  tabbedPane.addTab("FlatTopped: Flower", createFlowerPanel())
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

// Pointy-topped hexagons laid out row by row
private fun createRowsPanel(): Component {
  val evenCount = 2 * N - 1 // Buttons in even rows
  val oddCount = 2 * N // Buttons in odd  rows
  val layout = HoneycombRowsLayout(TOTAL_ROWS, evenCount, oddCount, BTN_GAP)

  // Calculate total button count and add them
  var totalButtons = 0
  for (r in 0..<TOTAL_ROWS) {
    totalButtons += if (r % 2 == 0) evenCount else oddCount
  }
  return createHexagonPanel(layout, totalButtons, HexagonOrientation.POINTY_TOPPED)
}

// Flat-topped hexagons laid out in a flower pattern: 1 center + 6 around
private fun createFlowerPanel(): Component {
  val layout = HoneycombFlowerLayout(BTN_GAP)
  return createHexagonPanel(layout, FLOWER_SIZE, HexagonOrientation.FLAT_TOPPED)
}

private fun createHexagonPanel(
  layout: LayoutManager,
  buttonCount: Int,
  orientation: HexagonOrientation,
): Component {
  val p = JPanel(layout)
  p.setBackground(PANEL_BGC)
  p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  for (i in 0..<buttonCount) {
    p.add(createHexagonButton(i, orientation))
  }
  return p
}

private fun createHexagonButton(
  i: Int,
  orientation: HexagonOrientation,
): HexagonButton {
  val btn = HexagonButton("ID: $i", orientation)
  btn.setBackground(BTN_BGC)
  btn.setForeground(Color.WHITE)
  return btn
}

// Hexagon orientation
// The circumradius R is always half of the longer side of the bounding box,
// so only the angle of the first vertex differs between the two orientations.
// PointyTopped: W = R * sqrt(3), H = R * 2 -> W < H (always), R = H / 2
// FlatTopped:   W = R * 2, H = R * sqrt(3) -> W > H (always), R = W / 2
private enum class HexagonOrientation(
  val startAngle: Double,
) {
  POINTY_TOPPED(-Math.PI / 2.0), // Start at 12 oclock
  FLAT_TOPPED(0.0), // Start at 3 oclock
}

// Hexagon button component
private class HexagonButton(
  text: String,
  private val orientation: HexagonOrientation,
) : JButton(text) {
  private val hexagonSize = Dimension()
  private var hexagon: Shape? = null
  private var isHovered = false
  private var hoverHandler: MouseListener? = null

  override fun updateUI() {
    removeMouseListener(hoverHandler)
    super.updateUI()
    setContentAreaFilled(false)
    setFocusPainted(false)
    setBorderPainted(false)
    setOpaque(false)
    hoverHandler = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent?) {
        isHovered = true
        repaint()
      }

      override fun mouseExited(e: MouseEvent?) {
        isHovered = false
        repaint()
      }
    }
    addMouseListener(hoverHandler)
  }

  // Recalculate the hexagon shape only when the component size has changed.
  private fun getHexagon(): Shape {
    val d = getSize()
    val s = hexagon
    return if (s == null || hexagonSize != d) {
      hexagonSize.size = d
      createHexagon().also { hexagon = it }
    } else {
      s
    }
  }

  // Create a hexagon that fits inside the component bounds.
  // The circumradius R equals half of the longer side of the bounding box,
  // so max(cx, cy) is used: min(cx, cy) would shrink the hexagon
  // and leave gaps on all sides.
  // The center and the radius must be double, not int: with int the center of
  // an even sized component is off by half a pixel and Polygon rounds every
  // vertex, so the rightmost vertex ends up outside the component bounds and
  // gets clipped while the leftmost one stays inside.
  // The radius is also reduced by half the stroke width, because
  // Graphics2D#draw(...) centers the stroke on the path and its outer half
  // would otherwise be clipped too.
  private fun createHexagon(): Shape {
    val cx = getWidth() / 2.0
    val cy = getHeight() / 2.0
    // val radius = min(cx, cy)
    val radius = max(cx, cy) - BORDER_WIDTH / 2.0
    val path = Path2D.Double()
    for (i in 0..<VERTICES) {
      // Start at the orientation angle, step by 60 degrees(PI/3)
      val angle = orientation.startAngle + i * Math.PI / 3.0
      val x = cx + radius * cos(angle)
      val y = cy + radius * sin(angle)
      if (i == 0) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }
    path.closePath()
    return path
  }

  // Hit-test against the hexagon shape, not the bounding rectangle.
  override fun contains(x: Int, y: Int) =
    getHexagon().contains(x.toDouble(), y.toDouble())

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val shape = getHexagon()

    // Choose fill color based on interaction state
    val bg = getBackground()
    if (getModel().isArmed) {
      g2.color = bg.darker()
    } else if (isHovered) {
      g2.color = bg.brighter()
    } else {
      g2.color = bg
    }
    g2.fill(shape) // Always fill

    // Draw border; glow effect on hover
    if (isHovered) {
      // Round join: a miter join would stick out further than BORDER_WIDTH / 2
      // at every vertex and get clipped by the component bounds
      g2.stroke = BasicStroke(
        BORDER_WIDTH,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND,
      )
      g2.color = Color(0x64_FF_FF_FF, true)
      g2.draw(shape)
      g2.stroke = BasicStroke(
        1.5f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND,
      )
      g2.color = Color.WHITE
      g2.draw(shape)
    } else {
      g2.stroke = BasicStroke(
        1f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND,
      )
      g2.color = bg.darker()
      g2.draw(shape)
    }

    g2.dispose()
    super.paintComponent(g) // Draw label text
  }

  companion object {
    private const val VERTICES = 6

    // Widest stroke used to draw the hexagon outline
    private const val BORDER_WIDTH = 3f
  }
}

// Base class of the honeycomb hexagon button layout managers
private abstract class AbstractHoneycombLayout(
  // Visual gap between adjacent hexagon edges, in pixels.
  // gap = 0 : edges touch perfectly
  // gap > 0 : uniform spacing
  protected val gap: Int,
) : LayoutManager {
  // Calculate the hexagon bounding box size that fits in the given area
  protected abstract fun getButtonSize(
    width: Int,
    height: Int,
  ): Dimension

  // Place all components in the given area using the given hexagon size
  protected abstract fun layoutHexagons(
    parent: Container,
    area: Rectangle,
    buttonSize: Dimension,
  )

  override fun layoutContainer(parent: Container) {
    if (parent.componentCount > 0 && parent is JComponent) {
      val area = SwingUtilities.calculateInnerArea(parent, null)
      layoutHexagons(parent, area, getButtonSize(area.width, area.height))
    }
  }

  override fun preferredLayoutSize(parent: Container) = Dimension(500, 400)

  override fun minimumLayoutSize(parent: Container) = Dimension(200, 150)

  override fun addLayoutComponent(
    name: String,
    comp: Component,
  ) {
    // not needed
  }

  override fun removeLayoutComponent(comp: Component) {
    // not needed
  }
}

// Pointy-topped hexagon button layout manager
// Row pattern
// Even rows (0, 2, ...): 2n-1 buttons, offset right by half cell width
// Odd  rows (1, 3, ...): 2n buttons, flush left
private class HoneycombRowsLayout(
  private val rows: Int,
  private val evenCols: Int, // Button count for even rows (2n-1)
  private val oddCols: Int, // Button count for odd rows (2n)
  gap: Int,
) : AbstractHoneycombLayout(gap) {
  override fun layoutHexagons(
    parent: Container,
    area: Rectangle,
    buttonSize: Dimension,
  ) {
    val slotW = buttonSize.width + gap // Horizontal pitch
    val slotH = buttonSize.height + gap // Vertical base

    // Center the grid inside the panel
    val gridW = oddCols * slotW
    val gridH = (slotH * (.25 + .75 * rows)).toInt()
    val marginX = area.x + (area.width - gridW) / 2
    val marginY = area.y + (area.height - gridH) / 2

    var compIdx = 0
    for (r in 0..<rows) {
      val isEvenRow = r % 2 == 0
      val colsInRow = if (isEvenRow) evenCols else oddCols

      // Y position: step by 75% of slot height
      val y = marginY + (r * slotH * .75 + gap / 2.0).toInt()
      // Even rows shift right by half a slot
      val rowOffsetX = if (isEvenRow) slotW / 2 else 0

      for (col in 0..<colsInRow) {
        if (compIdx >= parent.componentCount) {
          break
        }
        val c = parent.getComponent(compIdx)
        val x = marginX + rowOffsetX + col * slotW + gap / 2
        // Set position and size
        c.setBounds(x, y, buttonSize.width, buttonSize.height)
        compIdx += 1
      }
    }
  }

  override fun getButtonSize(
    width: Int,
    height: Int,
  ): Dimension {
    // Derive cellW,cellH from horizontal constraint
    val cwFromWidth = width.toDouble() / oddCols - gap
    val chFromWidth = cwFromWidth / RATIO

    // Derive cellW,cellH from vertical constraint
    val chFromHeight = height / (.25 + .75 * rows) - gap
    val cwFromHeight = chFromHeight * RATIO

    // Adopt the smaller to satisfy both constraints
    val cellW: Double
    val cellH: Double
    if (cwFromWidth <= cwFromHeight) {
      cellW = cwFromWidth
      cellH = chFromWidth
    } else {
      cellW = cwFromHeight
      cellH = chFromHeight
    }
    return Dimension(max(1, cellW.toInt()), max(1, cellH.toInt()))
  }
}

// Flat-topped hexagon button layout manager
// Flower pattern: 1 hexagon in the center and 6 hexagons around it
private class HoneycombFlowerLayout(
  gap: Int,
) : AbstractHoneycombLayout(gap) {
  override fun layoutHexagons(
    parent: Container,
    area: Rectangle,
    buttonSize: Dimension,
  ) {
    val w = buttonSize.width
    val h = buttonSize.height
    // Neighbor center offsets: 75% of the width, 50% of the height
    val dx = w * .75 + gap * RATIO
    val dy = h * .5 + gap * .5
    val slotH = (h + gap).toDouble()

    val centerX = area.x + area.width / 2
    val centerY = area.y + area.height / 2

    val positions = listOf(
      0.0 to 0.0,
      0.0 to -slotH,
      dx to -dy,
      dx to dy,
      0.0 to slotH,
      -dx to dy,
      -dx to -dy,
    )

    for (i in 0..<min(parent.componentCount, positions.size)) {
      val c = parent.getComponent(i)
      val cx = (centerX + positions[i].first).roundToInt()
      val cy = (centerY + positions[i].second).roundToInt()
      c.setBounds(cx - w / 2, cy - h / 2, w, h)
    }
  }

  override fun getButtonSize(
    width: Int,
    height: Int,
  ): Dimension {
    val widFromWidth = (width - 2.0 * gap * RATIO) / COLUMNS
    val widFromHeight = (height - 2.0 * gap) / (RATIO * LINES)
    val cellW = min(widFromWidth, widFromHeight)
    val cellH = cellW * RATIO
    return Dimension(
      max(1, cellW.roundToInt()),
      max(1, cellH.roundToInt()),
    )
  }

  companion object {
    // Hexagon widths that fit horizontally: 1 + 2 * .75 = 2.5
    private const val COLUMNS = 2.5

    // Hexagon heights that fit vertically: 3
    private const val LINES = 3.0
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
