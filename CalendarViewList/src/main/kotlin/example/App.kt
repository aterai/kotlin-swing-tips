package example

import java.awt.* // ktlint-disable no-wildcard-imports
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
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  val cellsz = Dimension(40, 26)
  val yearMonthLabel = JLabel("", SwingConstants.CENTER)
  val monthList: JList<LocalDate> = object : JList<LocalDate>() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setLayoutOrientation(JList.HORIZONTAL_WRAP)
      setVisibleRowCount(CalendarViewListModel.ROW_COUNT) // ensure 6 rows in the list
      setFixedCellWidth(cellsz.width)
      setFixedCellHeight(cellsz.height)
      setCellRenderer(CalendarListRenderer())
      getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
    }
  }
  val realLocalDate = LocalDate.now(ZoneId.systemDefault())
  private var currentLocalDate: LocalDate = realLocalDate

  init {
    installActions()

    val l = Locale.getDefault()
    val weekModel = DefaultListModel<DayOfWeek>()
    val firstDayOfWeek = WeekFields.of(l).getFirstDayOfWeek()
    for (i in 0 until DayOfWeek.values().size) {
      weekModel.add(i, firstDayOfWeek.plus(i.toLong()))
    }
    val header = JList<DayOfWeek>(weekModel)
    header.setLayoutOrientation(JList.HORIZONTAL_WRAP)
    header.setVisibleRowCount(0)
    header.setFixedCellWidth(cellsz.width)
    header.setFixedCellHeight(cellsz.height)
    header.setCellRenderer(object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        super.getListCellRendererComponent(list, value, index, false, false)
        setHorizontalAlignment(SwingConstants.CENTER)
        (value as? DayOfWeek)?.also {
          // val s = it.getDisplayName(TextStyle.SHORT_STANDALONE, l);
          // setText(s.substring(0, minOf(2, s.length())));
          setText(it.getDisplayName(TextStyle.SHORT_STANDALONE, l))
          setBackground(Color(0xDC_DC_DC))
        }
        return this
      }
    })
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
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

    val label = JLabel(" ", SwingConstants.CENTER)

    monthList.getSelectionModel().addListSelectionListener { e ->
      val lsm = e.getSource() as ListSelectionModel
      if (lsm.isSelectionEmpty()) {
        label.setText(" ")
      } else {
        val model = monthList.getModel()
        val from = model.getElementAt(lsm.getMinSelectionIndex())
        val to = model.getElementAt(lsm.getMaxSelectionIndex())
        label.setText(Period.between(from, to).toString())
      }
    }

    val box = Box.createVerticalBox()
    box.add(yearMonthPanel)
    box.add(Box.createVerticalStrut(2))
    box.add(scroll)
    box.add(label)

    add(box)
    setPreferredSize(Dimension(320, 240))
  }

  private fun installActions() {
    val im = monthList.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "selectNextIndex")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "selectPreviousIndex")

    val am = monthList.getActionMap()
    am.put("selectPreviousIndex", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val index = monthList.getLeadSelectionIndex()
        if (index > 0) {
          monthList.setSelectedIndex(index - 1)
        } else {
          val d = monthList.getModel().getElementAt(0).minusDays(1)
          updateMonthView(currentLocalDate.minusMonths(1))
          monthList.setSelectedValue(d, false)
        }
      }
    })
    am.put("selectNextIndex", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val index = monthList.getLeadSelectionIndex()
        if (index < monthList.getModel().getSize() - 1) {
          monthList.setSelectedIndex(index + 1)
        } else {
          val d = monthList.getModel().getElementAt(monthList.getModel().getSize() - 1).plusDays(1)
          updateMonthView(currentLocalDate.plusMonths(1))
          monthList.setSelectedValue(d, false)
        }
      }
    })
    val selectPreviousRow = am.get("selectPreviousRow")
    am.put("selectPreviousRow", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val index = monthList.getLeadSelectionIndex()
        val dowvl = DayOfWeek.values().size // 7
        if (index < dowvl) {
          val d = monthList.getModel().getElementAt(index).minusDays(dowvl.toLong())
          updateMonthView(currentLocalDate.minusMonths(1))
          monthList.setSelectedValue(d, false)
        } else {
          selectPreviousRow.actionPerformed(e)
        }
      }
    })
    val selectNextRow = am.get("selectNextRow")
    am.put("selectNextRow", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val index = monthList.getLeadSelectionIndex()
        val dowvl = DayOfWeek.values().size // 7
        if (index > monthList.getModel().getSize() - dowvl) {
          val d = monthList.getModel().getElementAt(index).plusDays(dowvl.toLong())
          updateMonthView(currentLocalDate.plusMonths(1))
          monthList.setSelectedValue(d, false)
        } else {
          selectNextRow.actionPerformed(e)
        }
      }
    })
  }

  fun updateMonthView(localDate: LocalDate) {
    currentLocalDate = localDate
    val dtf = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
    yearMonthLabel.setText(localDate.format(dtf))
    monthList.setModel(CalendarViewListModel(localDate))
  }

  private inner class CalendarListRenderer : ListCellRenderer<LocalDate> {
    private val renderer = DefaultListCellRenderer()

    override fun getListCellRendererComponent(
      list: JList<out LocalDate>,
      value: LocalDate,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      val l = c as? JLabel ?: return c
      l.setOpaque(true)
      l.setHorizontalAlignment(SwingConstants.CENTER)
      l.setText(value.getDayOfMonth().toString())
      val isSameMonth = YearMonth.from(value) == YearMonth.from(currentLocalDate)
      val fgc = if (isSameMonth) getForegroundColor(value) else Color.GRAY
      l.setForeground(if (isSelected) l.getForeground() else fgc)
      return l
    }

    private fun getForegroundColor(ld: LocalDate) = when {
      ld.isEqual(realLocalDate) -> Color(0x64_FF_64)
      else -> getDayOfWeekColor(ld.getDayOfWeek())
    }

    private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
      DayOfWeek.SUNDAY -> Color(0xFF_64_64)
      DayOfWeek.SATURDAY -> Color(0x64_64_FF)
      else -> Color.BLACK
    }
  }
}

internal class CalendarViewListModel(date: LocalDate) : AbstractListModel<LocalDate>() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val dowv = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    startDate = firstDayOfMonth.minusDays(dowv.toLong())
  }

  override fun getSize() = DayOfWeek.values().size * ROW_COUNT

  override fun getElementAt(index: Int) = startDate.plusDays(index.toLong())

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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
