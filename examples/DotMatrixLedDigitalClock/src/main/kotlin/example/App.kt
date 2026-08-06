package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*

private const val RADIX = 10
private const val BLOCK_GAP = 1
private const val TIMER_DELAY_MS = 100
private const val LIST_GAP = 10
private const val DIGIT_COLUMNS = 4
private const val DIGIT_ROWS = 7
private val HOUR_MIN_DOT_SIZE = Dimension(10, 10)
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
private var time = LocalTime.now(ZoneId.systemDefault())

fun createUI(): Component {
  val hoursMinutesModel = object : DefaultListModel<Boolean>() {
    override fun getElementAt(index: Int) = isHourMinuteDotLit(time, index)
  }
  hoursMinutesModel.setSize((DIGIT_COLUMNS * 4 + 5) * DIGIT_ROWS)
  val hoursMinutesList = createLedDotMatrixList(hoursMinutesModel, HOUR_MIN_DOT_SIZE)

  val secondsModel = object : DefaultListModel<Boolean>() {
    override fun getElementAt(index: Int) = isSecondDotLit(time, index)
  }
  secondsModel.setSize((DIGIT_COLUMNS * 2 + 1) * DIGIT_ROWS)
  val secondsList = createLedDotMatrixList(secondsModel, SECONDS_DOT_SIZE)

  timer.addActionListener {
    time = LocalTime.now(ZoneId.systemDefault())
    hoursMinutesList.repaint()
    secondsList.repaint()
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

private fun isDigitDotLit(
  index: Int,
  blockStart: Int,
  blockEnd: Int,
  digit: Int,
) = index < blockEnd * DIGIT_ROWS &&
  DIGIT_PATTERNS[digit].contains(index - blockStart * DIGIT_ROWS)

private fun isHourMinuteDotLit(time: LocalTime, index: Int): Boolean {
  val hour = time.hour
  val hourTens = hour / RADIX
  var blockStart = 0
  var blockEnd = DIGIT_COLUMNS
  // Blank the hour's leading zero: the tens digit only lights up when hour >= 10.
  var lit = isDigitDotLit(index, blockStart, blockEnd, hourTens) && hour >= RADIX

  val hourUnits = hour - hourTens * RADIX
  blockStart = blockEnd + BLOCK_GAP
  blockEnd = blockStart + DIGIT_COLUMNS
  lit = lit or isDigitDotLit(index, blockStart, blockEnd, hourUnits)

  // Blink the colon dots once per second, on for even seconds and off for odd seconds.
  val secondUnits = time.second % RADIX
  blockStart = blockEnd + BLOCK_GAP
  blockEnd = blockStart + BLOCK_GAP
  val b1 = index < blockEnd * DIGIT_ROWS
  val b2 = secondUnits % 2 == 0
  val b3 = COLON_DOT_ROWS.contains(index - blockStart * DIGIT_ROWS)
  lit = lit or (b1 && b2 && b3)

  val minute = time.minute
  val minuteTens = minute / RADIX
  blockStart = blockEnd + BLOCK_GAP
  blockEnd = blockStart + DIGIT_COLUMNS
  lit = lit or isDigitDotLit(index, blockStart, blockEnd, minuteTens)

  val minuteUnits = minute - minuteTens * RADIX
  blockStart = blockEnd + BLOCK_GAP
  blockEnd = blockStart + DIGIT_COLUMNS
  lit = lit or isDigitDotLit(index, blockStart, blockEnd, minuteUnits)

  return lit
}

private fun isSecondDotLit(
  time: LocalTime,
  index: Int,
): Boolean {
  val second = time.second
  val secondTens = second / RADIX
  var blockStart = 0
  var blockEnd = DIGIT_COLUMNS
  val lit = isDigitDotLit(index, blockStart, blockEnd, secondTens)

  val secondUnits = second - secondTens * RADIX
  blockStart = blockEnd + BLOCK_GAP
  blockEnd = blockStart + DIGIT_COLUMNS
  return lit || isDigitDotLit(index, blockStart, blockEnd, secondUnits)
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
