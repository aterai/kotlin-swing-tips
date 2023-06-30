package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*

val cellSize = Dimension(40, 26)
private val yearMonthLabel = JLabel("", SwingConstants.CENTER)
val monthList = object : JList<LocalDate>() {
  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    layoutOrientation = HORIZONTAL_WRAP
    visibleRowCount = CalendarViewListModel.ROW_COUNT // ensure 6 rows in the list
    fixedCellWidth = cellSize.width
    fixedCellHeight = cellSize.height
    cellRenderer = CalendarListRenderer()
    selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
  }
}
val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
private var currentLocalDate = realLocalDate

fun makeUI(): Component {
  installActions()

  val l = Locale.getDefault()
  val weekModel = DefaultListModel<DayOfWeek>()
  val firstDayOfWeek = WeekFields.of(l).firstDayOfWeek
  for (i in DayOfWeek.values().indices) {
    weekModel.add(i, firstDayOfWeek.plus(i.toLong()))
  }
  val header = object : JList<DayOfWeek>(weekModel) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      layoutOrientation = HORIZONTAL_WRAP
      visibleRowCount = 0
      fixedCellWidth = cellSize.width
      fixedCellHeight = cellSize.height
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

  val scroll = JScrollPane(monthList)
  scroll.setColumnHeaderView(header)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val label = JLabel(" ", SwingConstants.CENTER)

  monthList.selectionModel.addListSelectionListener { e ->
    label.text = (e.source as? ListSelectionModel)
      ?.takeUnless { it.isSelectionEmpty }
      ?.let {
        val model = monthList.model
        val from = model.getElementAt(it.minSelectionIndex)
        val to = model.getElementAt(it.maxSelectionIndex)
        Period.between(from, to).toString()
      }
      ?: " "
  }

  val box = Box.createVerticalBox()
  box.add(yearMonthPanel)
  box.add(Box.createVerticalStrut(2))
  box.add(scroll)
  box.add(label)

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

  val selectPreviousRow = am.get("selectPreviousRow")
  val a3 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = monthList.leadSelectionIndex
      val weekLength = DayOfWeek.values().size // 7
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

  val selectNextRow = am.get("selectNextRow")
  val a4 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = monthList.leadSelectionIndex
      val weekLength = DayOfWeek.values().size // 7
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
  currentLocalDate = localDate
  val dtf = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
  yearMonthLabel.text = localDate.format(dtf)
  monthList.model = CalendarViewListModel(localDate)
}

private class CalendarListRenderer : ListCellRenderer<LocalDate> {
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out LocalDate>,
    value: LocalDate,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    val isSameMonth = YearMonth.from(value) == YearMonth.from(currentLocalDate)
    val fgc = if (isSameMonth) getForegroundColor(value) else Color.GRAY
    c.foreground = if (isSelected) c.foreground else fgc
    if (c is JLabel) {
      c.isOpaque = true
      c.horizontalAlignment = SwingConstants.CENTER
      c.text = value.dayOfMonth.toString()
    }
    return c
  }

  private fun getForegroundColor(ld: LocalDate) = if (ld.isEqual(realLocalDate)) {
    Color(0x64_FF_64)
  } else {
    getDayOfWeekColor(ld.dayOfWeek)
  }

  private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xFF_64_64)
    DayOfWeek.SATURDAY -> Color(0x64_64_FF)
    else -> Color.BLACK
  }
}

private class CalendarViewListModel(date: LocalDate) : AbstractListModel<LocalDate>() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }

  override fun getSize() = DayOfWeek.values().size * ROW_COUNT

  override fun getElementAt(index: Int): LocalDate = startDate.plusDays(index.toLong())

  companion object {
    const val ROW_COUNT = 6
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
