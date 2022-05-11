package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import kotlin.math.roundToInt

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true

  val field = JTextField("5 : 3 : 2")
  val check = JCheckBox("ComponentListener#componentResized(...)", true)

  val scrollPane = JScrollPane(table)
  val cmpListener = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      if (check.isSelected) {
        setTableHeaderColumnRatio(table, field.text.trim())
      }
    }
  }
  scrollPane.addComponentListener(cmpListener)

  val button = JButton("revalidate")
  button.addActionListener {
    setTableHeaderColumnRatio(table, field.text.trim())
  }

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(JLabel("Ratio:"), BorderLayout.WEST)
  p.add(field)
  p.add(button, BorderLayout.EAST)

  val panel = JPanel(GridLayout(2, 1))
  panel.border = BorderFactory.createTitledBorder("JTableHeader column width ratio")
  panel.add(p)
  panel.add(check)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(panel, BorderLayout.NORTH)
    it.add(scrollPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setTableHeaderColumnRatio(table: JTable, text: String) {
  val m = table.columnModel
  val list = getWidthRatio(text, m.columnCount)
  var total = table.size.width
  val ratio = total / list.sum().toFloat()
  for (i in 0 until m.columnCount - 1) {
    val col = m.getColumn(i)
    val colWidth = (list[i] * ratio).roundToInt()
    col.preferredWidth = colWidth
    total -= colWidth
  }
  // m.getColumn(m.getColumnCount() - 1).setMaxWidth(total)
  m.getColumn(m.columnCount - 1).preferredWidth = total
  table.revalidate()
}

private fun getWidthRatio(text: String, length: Int) = runCatching {
  val a = text.split(":").toList().filter { it.trim().isNotEmpty() }.map { it.toInt() }
  val b = generateSequence(1) { it }.take(length).toList()
  (a + b).take(length)
}.onFailure {
  Toolkit.getDefaultToolkit().beep()
  val msg = "invalid value. ${it.message}"
  JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE)
}.getOrNull() ?: generateSequence(1) { it }.take(length).toList()

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
