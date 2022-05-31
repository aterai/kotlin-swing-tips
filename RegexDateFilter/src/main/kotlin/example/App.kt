package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.MouseEvent
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val cal = Calendar.getInstance().also {
    it.set(2002, Calendar.DECEMBER, 31, 10, 30, 15)
  }
  val date = cal.time
  cal.add(Calendar.DATE, -2)
  val start = cal.time
  cal.add(Calendar.DATE, 9)
  val end = cal.time
  println(date.toString()) // --> Tue Dec 31 10:30:15 JST 2002

  val data = arrayOf(arrayOf(date), arrayOf(start), arrayOf(end))
  val model = object : DefaultTableModel(data, arrayOf("Date")) {
    override fun getColumnClass(column: Int) = Date::class.java
  }
  val table = object : JTable(model) {
    override fun getToolTipText(e: MouseEvent) =
      getModel().getValueAt(convertRowIndexToModel(rowAtPoint(e.point)), 0).toString()
  }
  val sorter = TableRowSorter<TableModel>(model)
  table.rowSorter = sorter

  // RowFilter.regexFilter
  val m1 = Pattern.compile("12").matcher(date.toString())
  println(m1.find()) // --> false
  val m2 = Pattern.compile("Dec").matcher(date.toString())
  println(m2.find()) // --> true
  // a customized RegexFilter
  val m3 = Pattern.compile("12").matcher(DateFormat.getDateInstance().format(date))
  println(m3.find()) // --> true

  return JPanel(BorderLayout()).also {
    it.add(makeBox(sorter), BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeBox(sorter: TableRowSorter<TableModel>): Component {
  val field = JTextField("(?i)12")
  val r0 = JRadioButton("null", true)
  r0.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sorter.rowFilter = null
    }
  }
  val r1 = JRadioButton("RowFilter.regexFilter")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sorter.rowFilter = RowFilter.regexFilter(field.text)
    }
  }
  val r2 = JRadioButton("new RowFilter()")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sorter.rowFilter = RegexDateFilter(Pattern.compile(field.text))
    }
  }

  val p1 = JPanel(BorderLayout())
  p1.add(JLabel("regex:"), BorderLayout.WEST)
  p1.add(field)

  val p2 = JPanel()
  val bg = ButtonGroup()
  val box = Box.createVerticalBox() // JPanel(new GridLayout(2, 1, 5, 5));
  box.border = BorderFactory.createEmptyBorder(5, 2, 5, 2)
  listOf(r0, r1, r2).forEach {
    bg.add(it)
    p2.add(it)
  }
  box.add(p1)
  box.add(p2)
  return box
}

private class RegexDateFilter(pattern: Pattern) : RowFilter<TableModel?, Int?>() {
  private val matcher = pattern.matcher("")

  override fun include(entry: Entry<out TableModel?, out Int?>): Boolean {
    for (i in entry.valueCount - 1 downTo 0) {
      val v = entry.getValue(i)
      if (v is Date) {
        matcher.reset(DateFormat.getDateInstance().format(v))
      } else {
        matcher.reset(entry.getStringValue(i))
      }
      if (matcher.find()) {
        return true
      }
    }
    return false
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
