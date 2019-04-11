package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  val currentLocalDate = LocalDate.now()
  val weekList: JList<Contribution> = object : JList<Contribution>(CalendarViewListModel(currentLocalDate)) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setLayoutOrientation(JList.VERTICAL_WRAP)
      setVisibleRowCount(DayOfWeek.values().size) // ensure 7 rows in the list
      setFixedCellWidth(CELLSZ.width)
      setFixedCellHeight(CELLSZ.height)
      setCellRenderer(ContributionListRenderer())
      getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
      setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    }
  }
  val color = Color(50, 200, 50)
  val activityIcons = listOf(
      ColorIcon(Color(200, 200, 200)),
      ColorIcon(color.brighter()),
      ColorIcon(color),
      ColorIcon(color.darker()),
      ColorIcon(color.darker().darker()))

  init {
    val font = weekList.getFont().deriveFont(CELLSZ.height - 1f)

    val box = Box.createHorizontalBox()
    box.add(makeLabel("Less", font))
    box.add(Box.createHorizontalStrut(2))
    activityIcons.forEach { icon ->
      box.add(JLabel(icon))
      box.add(Box.createHorizontalStrut(2))
    }
    box.add(makeLabel("More", font))

    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createEmptyBorder(10, 2, 10, 2))
    p.setBackground(Color.WHITE)

    val c = GridBagConstraints()
    p.add(makeWeekCalendar(weekList, font), c)

    c.insets = Insets(10, 0, 2, 0)
    c.gridy = 1
    c.anchor = GridBagConstraints.LINE_END
    p.add(box, c)

    add(p, BorderLayout.NORTH)
    add(JScrollPane(JTextArea()))
    setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    setPreferredSize(Dimension(320, 240))
  }

  private inner class ContributionListRenderer : ListCellRenderer<Contribution> {
    private val renderer = DefaultListCellRenderer()

    override fun getListCellRendererComponent(
      list: JList<out Contribution>,
      value: Contribution,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val l = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
      if (value.date.isAfter(currentLocalDate)) {
        l.setIcon(ColorIcon(Color.WHITE))
        l.setToolTipText(null)
      } else {
        l.setIcon(activityIcons.get(value.activity))
        val actTxt = if (value.activity == 0) "No" else value.activity.toString()
        l.setToolTipText(actTxt + " contribution on " + value.date.toString())
      }
      return l
    }
  }

  private fun makeWeekCalendar(weekList: JList<Contribution>, font: Font): Component {
    val loc = Locale.getDefault()
    return JScrollPane(weekList).also {
      it.setBorder(BorderFactory.createEmptyBorder())
      it.setColumnHeaderView(makeColumnHeader(loc))
      it.setRowHeaderView(makeRowHeader(loc, font))
      it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
      it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
      it.setBackground(Color.WHITE)
    }
  }

  private fun makeRowHeader(loc: Locale, font: Font): Component {
    val weekFields = WeekFields.of(loc)
    val weekModel = DefaultListModel<String>()
    val firstDayOfWeek = weekFields.getFirstDayOfWeek()
    for (i in 0 until DayOfWeek.values().size) {
      val isEven = i % 2 == 0
      if (isEven) {
        weekModel.add(i, "")
      } else {
        weekModel.add(i, firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.SHORT_STANDALONE, loc))
      }
    }
    return JList<String>(weekModel).also {
      it.setEnabled(false)
      it.setFont(font)
      it.setLayoutOrientation(JList.VERTICAL_WRAP)
      it.setVisibleRowCount(DayOfWeek.values().size)
      it.setFixedCellHeight(CELLSZ.height)
    }
  }

  private fun makeColumnHeader(loc: Locale): Component {
    val colHeader = JPanel(GridBagLayout())
    colHeader.setBackground(Color.WHITE)
    val c = GridBagConstraints()
    c.gridx = 0
    while (c.gridx < CalendarViewListModel.WEEK_VIEW) {
      colHeader.add(Box.createHorizontalStrut(CELLSZ.width), c) // grid guides
      c.gridx++
    }
    c.anchor = GridBagConstraints.LINE_START
    c.gridy = 1
    c.gridwidth = 3 // use 3 columns to display the name of the month
    c.gridx = 0
    while (c.gridx < CalendarViewListModel.WEEK_VIEW - c.gridwidth + 1) {
      val date = weekList.getModel().getElementAt(c.gridx * DayOfWeek.values().size).date
      val isSimplyFirstWeekOfMonth = date.getMonth() != date.minusWeeks(1).getMonth()
      if (isSimplyFirstWeekOfMonth) {
        colHeader.add(makeLabel(date.getMonth().getDisplayName(TextStyle.SHORT, loc), font), c)
      }
      c.gridx++
    }
    return colHeader
  }

  private fun makeLabel(title: String, font: Font): JLabel {
    val label = JLabel(title)
    label.setFont(font)
    label.setEnabled(false)
    return label
  }

  companion object {
    val CELLSZ = Dimension(10, 10)
  }
}

class Contribution(val date: LocalDate, val activity: Int)

class CalendarViewListModel(date: LocalDate) : AbstractListModel<Contribution>() {
  private val startDate: LocalDate
  private val contributionActivity = mutableMapOf<LocalDate, Int>()

  init {
    val weekFields = WeekFields.of(Locale.getDefault())
    val dow = date.get(weekFields.dayOfWeek()) - 1
    // int wby = date.get(weekFields.weekOfWeekBasedYear())
    startDate = date.minusWeeks((WEEK_VIEW - 1).toLong()).minusDays(dow.toLong())
    val size = DayOfWeek.values().size * WEEK_VIEW
    (0 until size).forEach { contributionActivity.put(startDate.plusDays(it.toLong()), (0..4).random()) }
  }

  override fun getSize() = DayOfWeek.values().size * WEEK_VIEW

  override fun getElementAt(index: Int): Contribution {
    val date = startDate.plusDays(index.toLong())
    return Contribution(date, contributionActivity.get(date) ?: 0)
  }

  companion object {
    const val WEEK_VIEW = 27
  }
}

class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(0, 0, getIconWidth(), getIconHeight())
    g2.dispose()
  }

  override fun getIconWidth() = MainPanel.CELLSZ.width - 2

  override fun getIconHeight() = MainPanel.CELLSZ.height - 2
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
