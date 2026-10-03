package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.swing.*

private const val RADIX = 10
private const val BLOCK_GAP = 1
private const val TIMER_DELAY_MS = 100
private const val LIST_GAP = 10
private const val DIGIT_COLUMNS = 4
private const val DIGIT_ROWS = 7
private const val COLON_COLUMNS = 1

// H H : M M -> 4 digits, 1 colon and 4 gaps between the blocks
private const val HH_MM_COLUMNS = DIGIT_COLUMNS * 4 + COLON_COLUMNS + BLOCK_GAP * 4

// S S -> 2 digits and 1 gap
private const val SECONDS_COLUMNS = DIGIT_COLUMNS * 2 + BLOCK_GAP
private val HH_MM_DOT_SIZE = Dimension(10, 10)
private val SECONDS_DOT_SIZE = Dimension(8, 8)
private val DIGIT_PATTERNS = listOf(
  setOf(0, 1, 2, 3, 4, 5, 6, 7, 13, 14, 20, 21, 22, 23, 24, 25, 26, 27),
  setOf(21, 22, 23, 24, 25, 26, 27),
  setOf(0, 3, 4, 5, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 27),
  setOf(0, 3, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 10, 17, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 6, 7, 10, 13, 14, 17, 20, 21, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 13, 14, 17, 20, 21, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 7, 14, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 25, 26, 27),
  setOf(0, 1, 2, 3, 6, 7, 10, 13, 14, 17, 20, 21, 22, 23, 24, 25, 26, 27),
)
private val COLON_DOT_ROWS = listOf(2, 4)

private val timer = Timer(TIMER_DELAY_MS, null)
private var time = now()

fun createUI(): Component {
  val hoursMinutesList = createLedDotMatrixList(
    createDotMatrixModel(HH_MM_COLUMNS) { isHoursMinutesDotLit(time, it) },
    HH_MM_DOT_SIZE,
  )
  val secondsList = createLedDotMatrixList(
    createDotMatrixModel(SECONDS_COLUMNS) { isSecondsDotLit(time, it) },
    SECONDS_DOT_SIZE,
  )

  timer.addActionListener {
    // The display only changes once per second, so skip redundant repaints.
    val current = now()
    if (current != time) {
      time = current
      hoursMinutesList.repaint()
      secondsList.repaint()
    }
  }
  hoursMinutesList.alignmentY = Component.BOTTOM_ALIGNMENT
  secondsList.alignmentY = Component.BOTTOM_ALIGNMENT

  val box = Box.createHorizontalBox()
  box.add(hoursMinutesList)
  box.add(Box.createHorizontalStrut(LIST_GAP))
  box.add(secondsList)

  val p = object : JPanel(GridBagLayout()) {
    private var listener: HierarchyListener? = null

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
  }
  p.add(box)
  p.background = Color.BLACK
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun now() = LocalTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)

private fun createDotMatrixModel(
  columns: Int,
  isLit: (Int) -> Boolean,
) = object : AbstractListModel<Boolean>() {
  override fun getSize() = columns * DIGIT_ROWS

  override fun getElementAt(index: Int) = isLit(index)
}

// Every value in DIGIT_PATTERNS is within [0, DIGIT_COLUMNS * DIGIT_ROWS), so the
// index relative to a block that starts at another column is either negative or
// too large and simply misses the set: no bounds check is needed.
private fun isDigitDotLit(
  index: Int,
  startColumn: Int,
  digit: Int,
) = DIGIT_PATTERNS[digit].contains(index - startColumn * DIGIT_ROWS)

private fun isHoursMinutesDotLit(
  time: LocalTime,
  index: Int,
): Boolean {
  val hour = time.hour
  var column = 0
  // Blank the hour's leading zero: the tens digit only lights up when hour >= 10.
  var lit = hour >= RADIX && isDigitDotLit(index, column, hour / RADIX)

  column += DIGIT_COLUMNS + BLOCK_GAP
  lit = lit || isDigitDotLit(index, column, hour % RADIX)

  // Blink the colon dots once per second, on for even seconds and off for odd seconds.
  column += DIGIT_COLUMNS + BLOCK_GAP
  lit = lit || time.second % 2 == 0 &&
    COLON_DOT_ROWS.contains(index - column * DIGIT_ROWS)

  val minute = time.minute
  column += COLON_COLUMNS + BLOCK_GAP
  lit = lit || isDigitDotLit(index, column, minute / RADIX)

  column += DIGIT_COLUMNS + BLOCK_GAP
  return lit || isDigitDotLit(index, column, minute % RADIX)
}

private fun isSecondsDotLit(
  time: LocalTime,
  index: Int,
): Boolean {
  val second = time.second
  return isDigitDotLit(index, 0, second / RADIX) ||
    isDigitDotLit(index, DIGIT_COLUMNS + BLOCK_GAP, second % RADIX)
}

private fun createLedDotMatrixList(
  m: ListModel<Boolean>,
  d: Dimension,
) = object : JList<Boolean>(m) {
  override fun updateUI() {
    fixedCellWidth = d.width
    fixedCellHeight = d.height
    visibleRowCount = DIGIT_ROWS
    cellRenderer = null
    super.updateUI()
    layoutOrientation = VERTICAL_WRAP
    isFocusable = false
    cellRenderer = LedListCellRenderer(cellRenderer, d)
    border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    background = Color.BLACK
  }
}

private class LedListCellRenderer : ListCellRenderer<Boolean> {
  private var renderer: ListCellRenderer<in Boolean>
  private var onIcon: Icon
  private var offIcon: Icon

  constructor(renderer: ListCellRenderer<in Boolean>, size: Dimension) {
    this.renderer = renderer
    this.onIcon = LedDotIcon(true, size)
    this.offIcon = LedDotIcon(false, size)
  }

  override fun getListCellRendererComponent(
    list: JList<out Boolean>,
    value: Boolean?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component? {
    val c = renderer.getListCellRendererComponent(list, null, index, false, false)
    if (c is JLabel) {
      c.setIcon(if (value == true) onIcon else offIcon)
    }
    return c
  }
}

private class LedDotIcon(
  private val lit: Boolean,
  private val size: Dimension,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    // JList#setLayoutOrientation(VERTICAL_WRAP) + SynthLookAndFeel(Nimbus, GTK) bug???
    // g2.translate(x, y)
    g2.paint = if (lit) ON_COLOR else c.background
    g2.fillOval(0, 0, iconWidth - 1, iconHeight - 1)
    g2.dispose()
  }

  override fun getIconWidth() = size.width

  override fun getIconHeight() = size.height

  companion object {
    private val ON_COLOR = Color(0x32_FF_AA)
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
