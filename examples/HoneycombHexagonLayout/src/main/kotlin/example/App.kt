package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// n determines button count per row
private const val N = 2
private const val TOTAL_ROWS = 3

// Gap between adjacent hexagon edges in pixels
// 0 = perfectly touching, positive = gap
private const val BTN_GAP = 6
private val BTN_BGC = Color(70, 130, 180) // Steel blue

fun createUI(): Component {
  val evenCount = 2 * N - 1 // Buttons in even rows
  val oddCount = 2 * N // Buttons in odd  rows
  val p = JPanel(HoneycombLayout(TOTAL_ROWS, evenCount, oddCount, BTN_GAP))
  p.setBackground(Color(45, 45, 45))
  p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  // Calculate total button count and add them
  var totalButtons = 0
  for (r in 0..<TOTAL_ROWS) {
    val count = if (r % 2 == 0) evenCount else oddCount
    totalButtons += count
  }
  for (i in 0..<totalButtons) {
    p.add(createHexagonButton(i))
  }
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createHexagonButton(i: Int): HexagonButton {
  val btn = HexagonButton("ID: $i")
  btn.setBackground(BTN_BGC)
  btn.setForeground(Color.WHITE)
  return btn
}

// Hexagon button component
private class HexagonButton(
  text: String,
) : JButton(text) {
  private var hexagon: Polygon? = null
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

  // Recalculate the hexagon polygon to fill the component bounds exactly.
  // For a pointy-top regular hexagon, the bounding box satisfies:
  // W = R * sqrt(3), H = R * 2 -> W < H (always)
  // Therefore the circumradius R equals H/2 (= cy), NOT W/2 (= cx).
  // Using Math.min(cx, cy) would pick cx = W/2 < R, shrinking the hexagon
  // and leaving gaps on all sides.
  // Using Math.max(cx, cy) correctly picks cy = R, filling the bounds.
  private fun calculateHexagon() {
    hexagon = Polygon().also {
      val cx = getWidth() / 2
      val cy = getHeight() / 2
      // int radius = Math.min(cx, cy);
      val radius = max(cx, cy)
      for (i in 0..5) {
        // Start at -PI/2 (12 o'clock), step by 60°(PI/3)
        val angle = -Math.PI / 2 + i * Math.PI / 3
        it.addPoint(
          (cx + radius * cos(angle)).toInt(),
          (cy + radius * sin(angle)).toInt(),
        )
      }
    }
  }

  // Hit-test against the hexagon shape, not the bounding rectangle.
  override fun contains(x: Int, y: Int): Boolean {
    if (hexagon == null || hexagon?.getBounds()?.width != getWidth()) {
      calculateHexagon()
    }
    return hexagon?.contains(x, y) == true
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    calculateHexagon()

    // Choose fill color based on interaction state
    val bg = getBackground()
    if (getModel().isArmed) {
      g2.color = bg.darker()
    } else if (isHovered) {
      g2.color = bg.brighter()
    } else {
      g2.color = bg
    }
    g2.fillPolygon(hexagon) // Always fill

    // Draw border; glow effect on hover
    if (isHovered) {
      g2.stroke = BasicStroke(3f)
      g2.color = Color(255, 255, 255, 100)
      g2.drawPolygon(hexagon)
      g2.stroke = BasicStroke(1.5f)
      g2.color = Color.WHITE
      g2.drawPolygon(hexagon)
    } else {
      g2.stroke = BasicStroke(1f)
      g2.color = bg.darker()
      g2.drawPolygon(hexagon)
    }

    g2.dispose()
    super.paintComponent(g) // Draw label text
  }
}

// Honeycomb hexagon button layout manager
// Row pattern
// Even rows (0, 2, ...): 2n-1 buttons, offset right by half cell width
// Odd  rows (1, 3, ...): 2n buttons, flush left
private class HoneycombLayout(
  private val rows: Int,
  private val evenCols: Int,
  private val oddCols: Int,
  private val gap: Int,
) : LayoutManager {
  override fun layoutContainer(parent: Container) {
    val insets = parent.insets
    val maxWidth = parent.getWidth() - insets.left - insets.right
    val maxHeight = parent.getHeight() - insets.top - insets.bottom

    val buttonSize = getButtonSize(maxWidth, maxHeight)
    val slotW = buttonSize.width + gap // Horizontal pitch
    val slotH = buttonSize.height + gap // Vertical base

    // Center the grid inside the panel
    val gridW = oddCols * slotW
    val gridH = (slotH * (.25 + .75 * rows)).toInt()
    val marginX = insets.left + (maxWidth - gridW) / 2
    val marginY = insets.top + (maxHeight - gridH) / 2

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
        c.setBounds(x, y, buttonSize.width, buttonSize.height)
        compIdx += 1
      }
    }
  }

  private fun getButtonSize(maxWidth: Int, maxHeight: Int): Dimension {
    // Derive cellW,cellH from horizontal constraint
    val cwFromWidth = maxWidth.toDouble() / oddCols - gap
    val chFromWidth = cwFromWidth / RATIO

    // Derive cellW,cellH from vertical constraint
    val chFromHeight = maxHeight / (.25 + .75 * rows) - gap
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
    val buttonW = max(1, cellW.toInt())
    val buttonH = max(1, cellH.toInt())
    return Dimension(buttonW, buttonH)
  }

  override fun preferredLayoutSize(parent: Container): Dimension = Dimension(500, 400)

  override fun minimumLayoutSize(parent: Container): Dimension = Dimension(200, 150)

  override fun addLayoutComponent(name: String?, comp: Component) {
    // not needed
  }

  override fun removeLayoutComponent(comp: Component) {
    // not needed
  }

  companion object {
    private val RATIO = sqrt(3.0) / 2.0
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
