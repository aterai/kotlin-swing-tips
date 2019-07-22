package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  val currentLocalDate = LocalDate.now(ZoneId.systemDefault())
  val weekList = object : JList<Contribution>(CalendarViewListModel(currentLocalDate)) {
    @Transient
    private var tip: JToolTip? = null

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

    override fun getToolTipText(e: MouseEvent): String? {
      val p = e.getPoint()
      val idx = locationToIndex(p)
      val rect = getCellBounds(idx, idx)
      if (idx < 0 || !rect.contains(p.x, p.y)) {
        return null
      }
      val value = getModel().getElementAt(idx)
      val act = if (value.activity == 0) "No" else value.activity.toString()
      val date = value.date.toString()
      return "<html>$act contribution <span style='color:#C8C8C8'> on $date"
    }

    override fun getToolTipLocation(e: MouseEvent): Point? {
      val p = e.getPoint()
      val i = locationToIndex(p)
      val rect = getCellBounds(i, i)

      val toolTipText = getToolTipText(e)
      if (toolTipText != null) {
        val tip = createToolTip()
        tip?.setTipText(toolTipText)
        val d = tip?.getPreferredSize() ?: Dimension()
        val gap = 2
        return Point((rect.getCenterX() - d.width / 2.0).toInt(), rect.y - d.height - gap)
      }
      return null
    }

    override fun createToolTip(): JToolTip? {
      if (tip == null) {
        val tt = BalloonToolTip()
        tt.setComponent(this)
        tip = tt
      }
      return tip
    }
  }
  val color = Color(0x32_C8_32)
  val activityIcons = listOf(
    ContributionIcon(Color(0xC8_C8_C8)),
    ContributionIcon(color.brighter()),
    ContributionIcon(color),
    ContributionIcon(color.darker()),
    ContributionIcon(color.darker().darker()))

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
        l.setIcon(ContributionIcon(Color.WHITE))
      } else {
        l.setIcon(activityIcons.get(value.activity))
      }
      return l
    }
  }

  private fun makeWeekCalendar(list: JList<*>, font: Font) = JScrollPane(list).also {
    val loc = Locale.getDefault()
    it.setBorder(BorderFactory.createEmptyBorder())
    it.setColumnHeaderView(makeColumnHeader(loc))
    it.setRowHeaderView(makeRowHeader(loc, font))
    it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    it.setBackground(Color.WHITE)
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

  private fun makeLabel(title: String, font: Font) = JLabel(title).also {
    it.setFont(font)
    it.setEnabled(false)
  }

  companion object {
    val CELLSZ = Dimension(10, 10)
  }
}

data class Contribution(val date: LocalDate, val activity: Int)

class CalendarViewListModel(date: LocalDate) : AbstractListModel<Contribution>() {
  private val startDate: LocalDate
  private val displayDays: Int
  private val contributionActivity = mutableMapOf<LocalDate, Int>()

  init {
    val dow = date.get(WeekFields.of(Locale.getDefault()).dayOfWeek())
    this.startDate = date.minusWeeks((WEEK_VIEW - 1).toLong()).minusDays((dow - 1).toLong())
    this.displayDays = DayOfWeek.values().size * (WEEK_VIEW - 1) + dow
    (0 until displayDays).forEach { contributionActivity.put(startDate.plusDays(it.toLong()), (0..4).random()) }
  }

  override fun getSize() = displayDays

  override fun getElementAt(index: Int): Contribution {
    val date = startDate.plusDays(index.toLong())
    return Contribution(date, contributionActivity.get(date) ?: 0)
  }

  companion object {
    const val WEEK_VIEW = 27
  }
}

class ContributionIcon(private val color: Color) : Icon {
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

class BalloonToolTip : JToolTip() {
  private var listener: HierarchyListener? = null

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      val c = e.getComponent()
      if (e.getChangeFlags().toInt() and HierarchyEvent.SHOWING_CHANGED != 0 && c.isShowing()) {
        (SwingUtilities.getRoot(c) as? JWindow)?.setBackground(Color(0x0, true))
      }
    }
    addHierarchyListener(listener)
    setOpaque(false)
    setForeground(Color.WHITE)
    setBackground(Color(0xC8_00_00_00.toInt(), true))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5 + TRI_HEIGHT, 5))
  }

  override fun getPreferredSize() = super.getPreferredSize().also { it.height = 32 }

  protected override fun paintComponent(g: Graphics) {
    val s = makeBalloonShape()
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(getBackground())
    g2.fill(s)
    g2.dispose()
    super.paintComponent(g)
  }

  private fun makeBalloonShape(): Shape {
    val w = getWidth() - 1
    val h = getHeight() - TRI_HEIGHT - 1
    val r = 10f
    val cx = getWidth() / 2
    val triangle = Polygon()
    triangle.addPoint(cx - TRI_HEIGHT, h)
    triangle.addPoint(cx, h + TRI_HEIGHT)
    triangle.addPoint(cx + TRI_HEIGHT, h)
    val area = Area(RoundRectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat(), r, r))
    area.add(Area(triangle))
    return area
  }

  companion object {
    private const val TRI_HEIGHT = 4
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
