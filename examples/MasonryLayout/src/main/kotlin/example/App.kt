package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.ranges.random

private const val FIXED_COLUMNS = 3
private const val GAP = 5
private const val MIN_CARD_HEIGHT = 60
private const val MAX_CARD_HEIGHT = 220
private const val CARD_MIN_WIDTH = 140
private const val CARD_MAX_WIDTH = 260

private const val PASTEL_SATURATION = .45f
private const val PASTEL_BRIGHTNESS = .85f
private const val LUMINANCE_LIMIT = 150

private val nextNumber = AtomicInteger(1)
private val masonryPanel = MasonryPanel(MasonryLayout(FIXED_COLUMNS, GAP))

fun createUI() = JPanel(BorderLayout()).also {
  it.add(createToolBar(), BorderLayout.NORTH)
  it.add(createScrollPane())
  repeat(8) {
    addCard()
  }
  it.preferredSize = Dimension(320, 240)
}

private fun createToolBar(): JToolBar {
  val addButton = JButton("Add card")
  addButton.addActionListener { addCard() }

  val clearButton = JButton("Delete all")
  clearButton.addActionListener { clearCards() }

  val autoFillCheckBox = JCheckBox("Auto-fill")
  autoFillCheckBox.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    val colCnt = if (b) MasonryLayout.AUTO_FILL else FIXED_COLUMNS
    masonryPanel.setLayout(MasonryLayout(colCnt, GAP))
    masonryPanel.revalidate()
    masonryPanel.repaint()
  }

  val toolBar = JToolBar()
  toolBar.add(addButton)
  toolBar.add(clearButton)
  toolBar.add(autoFillCheckBox)
  return toolBar
}

private fun createScrollPane(): JScrollPane {
  masonryPanel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP))
  val hoverHandler: MouseAdapter = object : MouseAdapter() {
    override fun mouseMoved(e: MouseEvent) {
      setHoveredCard(masonryPanel.getComponentAt(e.getPoint()))
    }

    override fun mouseExited(e: MouseEvent) {
      if (!e.component.contains(e.getPoint())) {
        setHoveredCard(null)
      }
    }
  }
  masonryPanel.addMouseListener(hoverHandler)
  masonryPanel.addMouseMotionListener(hoverHandler)

  val scrollPane = JScrollPane(masonryPanel)
  scrollPane.setBorder(null)
  scrollPane.getVerticalScrollBar().setUnitIncrement(GAP * 5)
  return scrollPane
}

private fun setHoveredCard(hovered: Component?) {
  masonryPanel.components
    .filterIsInstance<MasonryCard>()
    .forEach { it.setCloseButtonVisible(it == hovered) }
}

private fun addCard() {
  masonryPanel.add(createCard(nextNumber.getAndIncrement()))
  masonryPanel.revalidate()
  masonryPanel.repaint()
}

private fun removeCard(card: Component?) {
  masonryPanel.remove(card)
  masonryPanel.revalidate()
  masonryPanel.repaint()
  EventQueue.invokeLater {
    val pt = masonryPanel.getMousePosition()
    setHoveredCard(if (pt == null) null else masonryPanel.getComponentAt(pt))
  }
}

private fun clearCards() {
  masonryPanel.removeAll()
  masonryPanel.revalidate()
  masonryPanel.repaint()
}

private fun createCard(number: Int): MasonryCard {
  val card = MasonryCard(number.toString())
  card.setFont(card.getFont().deriveFont(Font.BOLD, 18f))

  val background = randomPastelColor()
  card.setBackground(background)
  card.setForeground(readableForeground(background))
  card.setBorder(BorderFactory.createLineBorder(background.darker()))

  val bound = MAX_CARD_HEIGHT - MIN_CARD_HEIGHT + 1
  val height = MIN_CARD_HEIGHT + (0..bound).random()
  card.preferredSize = Dimension(0, height)
  card.minimumSize = Dimension(CARD_MIN_WIDTH, height)
  card.maximumSize = Dimension(CARD_MAX_WIDTH, Int.MAX_VALUE)

  card.addCloseActionListener { removeCard(card) }
  return card
}

private fun randomPastelColor(): Color = Color.getHSBColor(
  Random.nextFloat(),
  PASTEL_SATURATION,
  PASTEL_BRIGHTNESS,
)

private fun readableForeground(bg: Color): Color {
  val luminance = 0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue
  return if (luminance > LUMINANCE_LIMIT) Color.BLACK else Color.WHITE
}

private class MasonryCard(
  title: String,
) : JLabel(title, CENTER) {
  private val closeButton = object : JButton(CloseIcon()) {
    override fun updateUI() {
      super.updateUI()
      setBorder(BorderFactory.createEmptyBorder())
      setBorderPainted(false)
      setContentAreaFilled(false)
      setFocusPainted(false)
      setFocusable(false)
      setToolTipText("Delete this card")
      isVisible = false
    }
  }

  init {
    setOpaque(true)
    setLayout(FlowLayout(FlowLayout.RIGHT, PADDING, PADDING))
    add(closeButton)
  }

  override fun setForeground(fg: Color) {
    super.setForeground(fg)
    EventQueue.invokeLater {
      closeButton.setForeground(fg)
    }
  }

  fun setCloseButtonVisible(flag: Boolean) {
    closeButton.isVisible = flag
  }

  fun addCloseActionListener(listener: ActionListener?) {
    closeButton.addActionListener(listener)
  }

  companion object {
    private const val PADDING = 2
  }
}

private class CloseIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    g2.paint = c.getForeground()
    g2.stroke = BasicStroke(2f)
    val end = SIZE - MARGIN - 1
    g2.drawLine(MARGIN, MARGIN, end, end)
    g2.drawLine(end, MARGIN, MARGIN, end)
    g2.dispose()
  }

  override fun getIconWidth() = SIZE

  override fun getIconHeight() = SIZE

  companion object {
    private const val SIZE = 12
    private const val MARGIN = 3
  }
}

private class MasonryPanel(
  layout: MasonryLayout,
) : JPanel(layout),
  Scrollable {
  override fun getPreferredScrollableViewportSize(): Dimension? = getPreferredSize()

  override fun getScrollableUnitIncrement(
    visibleRect: Rectangle?,
    orientation: Int,
    direction: Int,
  ) = UNIT_INCREMENT

  override fun getScrollableBlockIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = if (orientation == SwingConstants.VERTICAL) {
    visibleRect.height
  } else {
    visibleRect.width
  }

  override fun getScrollableTracksViewportWidth() = true

  override fun getScrollableTracksViewportHeight() = false

  companion object {
    private const val UNIT_INCREMENT = 24
  }
}

private class MasonryLayout(
  columnCount: Int,
  gap: Int,
) : LayoutManager {
  private val columnCount: Int
  private val gap: Int
  val isAutoFill
    get() = columnCount == AUTO_FILL

  init {
    require(columnCount >= 0) { "columnCount must be >= 0: $columnCount" }
    require(gap >= 0) { "gap must be >= 0: $gap" }
    this.columnCount = columnCount
    this.gap = gap
  }

  override fun addLayoutComponent(name: String?, comp: Component?) {
    // Do nothing because add without constraints
  }

  override fun removeLayoutComponent(comp: Component?) {
    // Does nothing because no cache is maintained
  }

  override fun preferredLayoutSize(parent: Container): Dimension {
    synchronized(parent.treeLock) {
      val columnHeights = placeComponents(parent, false)
      val insets = parent.insets
      var maxHeight = 0
      for (height in columnHeights) {
        maxHeight = max(maxHeight, height)
      }
      val width = resolveAvailableWidth(parent) + insets.left + insets.right
      return Dimension(width, maxHeight + insets.top + insets.bottom)
    }
  }

  override fun minimumLayoutSize(
    parent: Container,
  ) = preferredLayoutSize(parent)

  override fun layoutContainer(parent: Container) {
    synchronized(parent.treeLock) {
      placeComponents(parent, true)
    }
  }

  private fun resolveAvailableWidth(parent: Container): Int {
    val insets = parent.insets
    val actualWidth = parent.getWidth() - insets.left - insets.right
    val assumedColumns = if (this.isAutoFill) FALLBACK_COLUMNS else columnCount
    val assumedWidth = assumedColumns * FALLBACK_WIDTH + gap * (assumedColumns - 1)
    return if (actualWidth > 0) actualWidth else assumedWidth
  }

  private fun resolveColumnCount(parent: Container, availableWidth: Int): Int {
    var columns = columnCount
    if (this.isAutoFill) {
      // The minimum width is at least 1px to avoid division by zero.
      var minWidth = 1
      var hasVisible = false
      for (comp in parent.components) {
        if (comp.isVisible) {
          hasVisible = true
          minWidth = max(minWidth, comp.minimumSize.width)
        }
      }
      columns = if (hasVisible) (availableWidth + gap) / (minWidth + gap) else 1
    }
    return max(1, columns)
  }

  private fun resolveMaxColumnWidth(parent: Container): Int {
    var maxWidth = Int.MAX_VALUE
    for (comp in parent.components) {
      if (!comp.isVisible) {
        continue
      }
      maxWidth = min(maxWidth, comp.getMaximumSize().width)
    }
    return maxWidth
  }

  private fun placeComponents(parent: Container, apply: Boolean): IntArray {
    val insets = parent.insets
    val availableWidth = resolveAvailableWidth(parent)
    val resolvedColCnt = resolveColumnCount(parent, availableWidth)
    val cw = (availableWidth - gap * (resolvedColCnt - 1)) / resolvedColCnt
    var columnWidth = max(0, cw)
    if (this.isAutoFill) {
      columnWidth = min(columnWidth, resolveMaxColumnWidth(parent))
    }

    val columnHeights = IntArray(resolvedColCnt)

    for (comp in parent.components) {
      if (!comp.isVisible) {
        continue
      }
      val col = shortestColumn(*columnHeights)
      val x = insets.left + col * (columnWidth + gap)
      val y = insets.top + columnHeights[col]
      val height = comp.preferredSize.height

      if (apply) {
        comp.setBounds(x, y, columnWidth, height)
      }
      columnHeights[col] += height + gap
    }

    for (i in columnHeights.indices) {
      if (columnHeights[i] > 0) {
        columnHeights[i] -= gap
      }
    }
    return columnHeights
  }

  private fun shortestColumn(vararg columnHeights: Int): Int {
    var minIndex = 0
    for (i in 1..<columnHeights.size) {
      if (columnHeights[i] < columnHeights[minIndex]) {
        minIndex = i
      }
    }
    return minIndex
  }

  companion object {
    const val AUTO_FILL = 0
    private const val FALLBACK_WIDTH = 150
    private const val FALLBACK_COLUMNS = 3
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
