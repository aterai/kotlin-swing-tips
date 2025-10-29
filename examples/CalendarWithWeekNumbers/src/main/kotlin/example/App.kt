package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.*

private val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
private var currentLocalDate = realLocalDate
private val yearMonthLabel = JLabel("", SwingConstants.CENTER)
private val monthList = MonthList(realLocalDate)
private val weekNumberList = WeekNumberList()

fun makeUI(): Component {
  installActions()
  val l = Locale.getDefault()
  val weekModel = DefaultListModel<DayOfWeek>()
  val firstDayOfWeek = WeekFields.of(l).firstDayOfWeek
  DayOfWeek.entries.forEachIndexed { idx, _ ->
    weekModel.add(idx, firstDayOfWeek.plus(idx.toLong()))
  }
  val header = object : JList<DayOfWeek>(weekModel) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      layoutOrientation = HORIZONTAL_WRAP
      visibleRowCount = 0
      fixedCellWidth = monthList.fixedCellWidth
      fixedCellHeight = monthList.fixedCellHeight
      val renderer = cellRenderer
      setCellRenderer { list, value, index, _, _ ->
        renderer.getListCellRendererComponent(list, value, index, false, false).also {
          (it as? JLabel)?.also { label ->
            label.horizontalAlignment = SwingConstants.CENTER
            label.text = value.getDisplayName(TextStyle.SHORT_STANDALONE, l)
            label.background = Color(0xDC_DC_DC)
          }
        }
      }
    }
  }
  updateMonthView(realLocalDate)

  val prev = JButton("<")
  prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1)) }

  val next = JButton(">")
  next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1)) }

  val yearMonthPanel = JPanel(BorderLayout())
  yearMonthPanel.add(yearMonthLabel)
  yearMonthPanel.add(prev, BorderLayout.WEST)
  yearMonthPanel.add(next, BorderLayout.EAST)

  val scroll = object : JScrollPane(monthList) {
    override fun updateUI() {
      super.updateUI()
      setColumnHeaderView(header)
      setRowHeaderView(weekNumberList)
      verticalScrollBarPolicy = VERTICAL_SCROLLBAR_NEVER
      horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
      EventQueue.invokeLater {
        val p = SwingUtilities.getUnwrappedParent(this)
        p?.revalidate()
      }
    }
  }

  val box = Box.createVerticalBox()
  box.add(yearMonthPanel)
  box.add(Box.createVerticalStrut(2))
  box.add(scroll)

  return JPanel().also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun installActions() {
  val im = monthList.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "selectNextIndex")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "selectPreviousIndex")

  val am = monthList.actionMap
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = monthList.leadSelectionIndex
      if (index > 0) {
        monthList.setSelectedIndex(index - 1)
      } else {
        val d = monthList.model.getElementAt(0).minusDays(1)
        updateMonthView(currentLocalDate.minusMonths(1))
        monthList.setSelectedValue(d, false)
      }
    }
  }
  am.put("selectPreviousIndex", a1)

  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = monthList.leadSelectionIndex
      if (index < monthList.model.size - 1) {
        monthList.setSelectedIndex(index + 1)
      } else {
        val d = monthList.model.getElementAt(monthList.model.size - 1).plusDays(1)
        updateMonthView(currentLocalDate.plusMonths(1))
        monthList.setSelectedValue(d, false)
      }
    }
  }
  am.put("selectNextIndex", a2)

  val selectPreviousRow = am["selectPreviousRow"]
  val a3 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = monthList.leadSelectionIndex
      val weekLength = DayOfWeek.entries.size // 7
      if (index < weekLength) {
        val d = monthList.model.getElementAt(index).minusDays(weekLength.toLong())
        updateMonthView(currentLocalDate.minusMonths(1))
        monthList.setSelectedValue(d, false)
      } else {
        selectPreviousRow.actionPerformed(e)
      }
    }
  }
  am.put("selectPreviousRow", a3)

  val selectNextRow = am["selectNextRow"]
  val a4 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = monthList.leadSelectionIndex
      val weekLength = DayOfWeek.entries.size // 7
      if (index > monthList.model.size - weekLength) {
        val d = monthList.model.getElementAt(index).plusDays(weekLength.toLong())
        updateMonthView(currentLocalDate.plusMonths(1))
        monthList.setSelectedValue(d, false)
      } else {
        selectNextRow.actionPerformed(e)
      }
    }
  }
  am.put("selectNextRow", a4)
}

fun updateMonthView(localDate: LocalDate) {
  val locale = Locale.getDefault()
  val fmt = CalendarUtils.getLocalizedYearMonthFormatter(locale)
  val txt = localDate.format(fmt.withLocale(locale))
  yearMonthLabel.setText(CalendarUtils.getLocalizedYearMonthSeparator(txt))
  currentLocalDate = localDate
  monthList.setCurrentLocalDate(localDate)
  weekNumberList.model = WeekNumberListModel(localDate)
}

private class CalendarViewListModel(
  date: LocalDate,
) : AbstractListModel<LocalDate>() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }

  override fun getSize() = DayOfWeek.entries.size * ROW_COUNT

  override fun getElementAt(index: Int): LocalDate = startDate.plusDays(index.toLong())

  companion object {
    const val ROW_COUNT = 6
  }
}

private class MonthList(
  private var currentLocalDate: LocalDate,
) : JList<LocalDate>() {
  val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())

  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    visibleRowCount = CalendarViewListModel.ROW_COUNT
    fixedCellWidth = 40
    fixedCellHeight = 26
    cellRenderer = CalendarListRenderer()
    selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
  }

  fun setCurrentLocalDate(date: LocalDate) {
    currentLocalDate = date
    model = CalendarViewListModel(date)
  }

  private inner class CalendarListRenderer : ListCellRenderer<LocalDate> {
    private val renderer = DefaultListCellRenderer()

    override fun getListCellRendererComponent(
      list: JList<out LocalDate>,
      value: LocalDate,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean,
    ): Component {
      val c = renderer.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      )
      val m1 = YearMonth.from(value).monthValue
      val m2 = YearMonth.from(currentLocalDate).monthValue
      val isSameMonth = m1 == m2
      val fgc = if (isSameMonth) getForegroundColor(value) else Color.GRAY
      c.foreground = if (isSelected) c.foreground else fgc
      if (c is JLabel) {
        c.isOpaque = true
        c.horizontalAlignment = SwingConstants.CENTER
        c.text = value.dayOfMonth.toString()
      }
      return c
    }

    private fun getForegroundColor(ld: LocalDate) =
      if (ld.isEqual(realLocalDate)) {
        Color(0x64_FF_64)
      } else {
        getDayOfWeekColor(ld.dayOfWeek)
      }

    private fun getDayOfWeekColor(dow: DayOfWeek) =
      when (dow) {
        DayOfWeek.SUNDAY -> Color(0xFF_64_64)
        DayOfWeek.SATURDAY -> Color(0x64_64_FF)
        else -> Color.BLACK
      }
  }
}

private class WeekHeaderList :
  JList<DayOfWeek>(
    makeDayOfWeekListModel(),
  ) {
  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    val bgc = UIManager.getColor("TableHeader.background").darker()
    val r = cellRenderer
    setCellRenderer { list, value, index, _, _ ->
      r.getListCellRendererComponent(list, value, index, false, false).also {
        it.setBackground(bgc)
        if (it is JLabel) {
          it.setHorizontalAlignment(SwingConstants.CENTER)
          val l = Locale.getDefault()
          it.setText(value.getDisplayName(TextStyle.SHORT_STANDALONE, l))
        }
      }
    }
    selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
    setLayoutOrientation(HORIZONTAL_WRAP)
    setVisibleRowCount(0)
    setFixedCellWidth(CELL_SIZE.width)
    setFixedCellHeight(CELL_SIZE.height)
  }

  companion object {
    val CELL_SIZE = Dimension(38, 26)

    private fun makeDayOfWeekListModel(): ListModel<DayOfWeek> {
      val weekModel = DefaultListModel<DayOfWeek>()
      val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
      for (i in DayOfWeek.entries.toTypedArray().indices) {
        weekModel.add(i, firstDayOfWeek.plus(i.toLong()))
      }
      return weekModel
    }
  }
}

private class WeekNumberList : JList<Int>() {
  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setVisibleRowCount(CalendarViewListModel.ROW_COUNT)
    setFixedCellWidth(WeekHeaderList.CELL_SIZE.width)
    setFixedCellHeight(WeekHeaderList.CELL_SIZE.height)
    setFocusable(false)
    val bgc = UIManager.getColor("TableHeader.background").darker()
    val r = cellRenderer
    setCellRenderer { list, value, index, _, _ ->
      r.getListCellRendererComponent(list, value, index, false, false).also {
        it.setBackground(bgc)
        if (it is JLabel) {
          it.setHorizontalAlignment(SwingConstants.CENTER)
          it.setText(value?.toString() ?: "")
        }
      }
    }
  }
}

private class WeekNumberListModel(
  date: LocalDate,
) : AbstractListModel<Int>() {
  private val weekFields = WeekFields.of(Locale.getDefault())
  private val firstDayOfMonth = YearMonth.from(date).atDay(1)

  override fun getSize() = CalendarViewListModel.ROW_COUNT

  override fun getElementAt(index: Int) =
    firstDayOfMonth.plusWeeks(index.toLong()).get(weekFields.weekOfWeekBasedYear())
}

private object CalendarUtils {
  fun getLocalizedPattern(
    locale: Locale,
  ): String = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
    FormatStyle.LONG,
    null,
    Chronology.ofLocale(locale),
    locale,
  )

  fun getLocalizedYearMonthFormatter(locale: Locale): DateTimeFormatter {
    val localizedPattern = getLocalizedPattern(locale)
    val year = find(localizedPattern, Pattern.compile("(y+)"))
    val month = find(localizedPattern, Pattern.compile("(M+)"))
    val pattern = if (isYearFirst(locale)) "$year $month" else "$month $year"
    return DateTimeFormatter.ofPattern(pattern)
  }

  fun getLocalizedYearMonthSeparator(str: String): String {
    val list = str
      .split(" ".toRegex())
      .dropLastWhile { it.isEmpty() }
      .toTypedArray()
    val isNumeric = list.all { it.toIntOrNull() != null }
    val separator = if (isNumeric) " / " else " "
    return list.joinToString(separator)
  }

  fun find(str: String, ptn: Pattern): String {
    val matcher = ptn.matcher(str)
    return if (matcher.find()) matcher.group(1) else ""
  }

  fun isYearFirst(locale: Locale): Boolean {
    val localizedPattern = getLocalizedPattern(locale)
    val yearIndex = localizedPattern.indexOf('y')
    val monthIndex = localizedPattern.indexOf('M')
    return yearIndex != -1 && monthIndex != -1 && yearIndex < monthIndex
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
