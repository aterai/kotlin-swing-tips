package example

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val r1 = JRadioButton("prevent KeyStroke autoStartsEdit")
  val r2 = JRadioButton("prevent mouse from starting edit")
  val r3 = JRadioButton("start cell editing only F2")
  val r4 = JRadioButton("isCellEditable return false")

  val columnNames = arrayOf("CellEditable:false", "Integer", "String")
  val data = arrayOf(
    arrayOf("aaa", 12, "eee"),
    arrayOf("bbb", 5, "ggg"),
    arrayOf("CCC", 92, "fff"),
    arrayOf("DDD", 0, "hhh")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = if (column == 1) {
      Number::class.java
    } else {
      Any::class.java
    }

    override fun isCellEditable(row: Int, col: Int) = col != 0 && !r4.isSelected
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.setShowGrid(false)
  table.putClientProperty("terminateEditOnFocusLost", true)

  val al = ActionListener { e ->
    val rb = e.source
    table.putClientProperty("JTable.autoStartsEdit", rb != r1 && rb != r3)
    val cc = if (rb == r2 || rb == r3) Int.MAX_VALUE else 2
    (table.getDefaultEditor(Any::class.java) as? DefaultCellEditor)?.clickCountToStart = cc
    (table.getDefaultEditor(Number::class.java) as? DefaultCellEditor)?.clickCountToStart = cc
  }
  val p = Box.createVerticalBox()
  val bg = ButtonGroup()
  listOf(JRadioButton("default", true), r1, r2, r3, r4)
    .forEach { b ->
      b.addActionListener(al)
      bg.add(b)
      p.add(b)
    }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
