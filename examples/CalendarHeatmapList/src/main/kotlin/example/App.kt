package example

import java.awt.*
import java.awt.event.MouseEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*

private val CELL_SIZE = Dimension(10, 10)
private val color = Color(0x32_C8_32)
private val activityIcons = listOf(
  ContributionIcon(Color(0xC8_C8_C8)),
  ContributionIcon(color.brighter()),
  ContributionIcon(color),
  ContributionIcon(color.darker()),
  ContributionIcon(color.darker().darker()),
)
private val currentLocalDate = LocalDate.now(ZoneId.systemDefault())
private val weekList = object : JList<Contribution>(
  CalendarViewListModel(currentLocalDate),
) {
  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    layoutOrientation = VERTICAL_WRAP
    visibleRowCount = DayOfWeek.entries.size // ensure 7 rows in the list
    fixedCellWidth = CELL_SIZE.width
    fixedCellHeight = CELL_SIZE.height
    val renderer = cellRenderer
    setCellRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer
        .getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          (it as? JLabel)?.icon = if (value.date.isAfter(currentLocalDate)) {
            ContributionIcon(Color.WHITE)
          } else {
            activityIcons[value.activity]
          }
        }
    }
    selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
    border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val pt = e.point
    val idx = locationToIndex(pt)
    val rect = getCellBounds(idx, idx)
    return if (rect?.contains(pt) == true) {
      val value = model.getElementAt(idx)
      val act = if (value.activity == 0) "No" else value.activity.toString()
      val date = value.date.toString()
      "$act contribution on $date"
    } else {
      null
    }
  }
}

fun makeUI(): Component {
  val font = weekList.font.deriveFont(CELL_SIZE.height - 1f)

  val box = Box.createHorizontalBox()
  box.add(makeLabel("Less", font))
  box.add(Box.createHorizontalStrut(2))
  activityIcons.forEach { icon ->
    box.add(JLabel(icon))
    box.add(Box.createHorizontalStrut(2))
  }
  box.add(makeLabel("More", font))

  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createEmptyBorder(10, 2, 10, 2)
  p.background = Color.WHITE

  val c = GridBagConstraints()
  p.add(makeWeekCalendar(weekList, font), c)

  c.insets = Insets(10, 0, 2, 0)
  c.gridy = 1
  c.anchor = GridBagConstraints.LINE_END
  p.add(box, c)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeWeekCalendar(
  list: JList<*>,
  font: Font,
): Component {
  val loc = Locale.getDefault()
  val scroll = JScrollPane(list)
  scroll.setColumnHeaderView(makeColumnHeader(loc))
  scroll.setRowHeaderView(makeRowHeader(loc, font))
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  scroll.background = Color.WHITE
  return scroll
}

private fun makeRowHeader(
  loc: Locale,
  font: Font,
): Component {
  val weekFields = WeekFields.of(loc)
  val weekModel = DefaultListModel<String>()
  val firstDayOfWeek = weekFields.firstDayOfWeek
  DayOfWeek.entries.forEachIndexed { idx, _ ->
    val isEven = idx % 2 == 0
    if (isEven) {
      weekModel.add(idx, "")
    } else {
      val week = firstDayOfWeek.plus(idx.toLong())
      weekModel.add(idx, week.getDisplayName(TextStyle.SHORT_STANDALONE, loc))
    }
  }
  return JList(weekModel).also {
    it.isEnabled = false
    it.font = font
    it.layoutOrientation = JList.VERTICAL_WRAP
    it.visibleRowCount = DayOfWeek.entries.size
    it.setFixedCellHeight(CELL_SIZE.height)
  }
}

private fun makeColumnHeader(loc: Locale): Component {
  val colHeader = JPanel(GridBagLayout())
  colHeader.background = Color.WHITE
  val c = GridBagConstraints()
  c.gridx = 0
  while (c.gridx < CalendarViewListModel.WEEK_VIEW) {
    colHeader.add(Box.createHorizontalStrut(CELL_SIZE.width), c) // grid guides
    c.gridx++
  }
  c.anchor = GridBagConstraints.LINE_START
  c.gridy = 1
  c.gridwidth = 3 // use 3 columns to display the name of the month
  c.gridx = 0
  while (c.gridx < CalendarViewListModel.WEEK_VIEW - c.gridwidth + 1) {
    val date = weekList.model.getElementAt(c.gridx * DayOfWeek.entries.size).date
    val isSimplyFirstWeekOfMonth = date.month != date.minusWeeks(1).month
    if (isSimplyFirstWeekOfMonth) {
      val title = date.month.getDisplayName(TextStyle.SHORT, loc)
      colHeader.add(makeLabel(title, colHeader.font), c)
    }
    c.gridx++
  }
  return colHeader
}

private fun makeLabel(title: String, font: Font) = JLabel(title).also {
  it.font = font
  it.isEnabled = false
}

private data class Contribution(
  val date: LocalDate,
  val activity: Int,
)

private class CalendarViewListModel(
  date: LocalDate,
) : AbstractListModel<Contribution>() {
  private val startDate: LocalDate
  private val displayDays: Int
  private val contributionActivity = mutableMapOf<LocalDate, Int>()

  init {
    val dow = date[WeekFields.of(Locale.getDefault()).dayOfWeek()]
    val minusWeeks = (WEEK_VIEW - 1).toLong()
    val minusDays = (dow - 1).toLong()
    this.startDate = date.minusWeeks(minusWeeks).minusDays(minusDays)
    this.displayDays = DayOfWeek.entries.size * (WEEK_VIEW - 1) + dow
    for (i in 0..<displayDays) {
      contributionActivity[startDate.plusDays(i.toLong())] = (0..4).random()
    }
  }

  override fun getSize() = displayDays

  override fun getElementAt(index: Int): Contribution {
    val date = startDate.plusDays(index.toLong())
    return Contribution(date, contributionActivity[date] ?: 0)
  }

  companion object {
    const val WEEK_VIEW = 27
  }
}

private class ContributionIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    // JList#setLayoutOrientation(VERTICAL_WRAP) + SynthLookAndFeel(Nimbus, GTK) bug???
    // g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = CELL_SIZE.width - 2

  override fun getIconHeight() = CELL_SIZE.height - 2
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
