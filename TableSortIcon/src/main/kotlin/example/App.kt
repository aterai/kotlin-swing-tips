package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.IconUIResource
import javax.swing.table.DefaultTableModel

private val EMPTY_ICON = EmptyIcon()

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

  val clearButton = JButton("clear SortKeys")
  clearButton.addActionListener {
    table.rowSorter.setSortKeys(null)
  }

  return JPanel(BorderLayout()).also {
    it.add(makeRadioPane(table), BorderLayout.NORTH)
    it.add(clearButton, BorderLayout.SOUTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRadioPane(table: JTable): Box {
  val cl = Thread.currentThread().contextClassLoader
  val ascendingPath = cl.getResource("example/ascending.png")
  val descendingPath = cl.getResource("example/descending.png")
  val r0 = JRadioButton("Default", true)
  val r1 = JRadioButton("Empty")
  val r2 = JRadioButton("Custom")
  val al = ActionListener { e ->
    val ascending: Icon
    val descending: Icon
    when (e.source as? JRadioButton) {
      r0 -> {
        ascending = UIManager.getLookAndFeelDefaults().getIcon("Table.ascendingSortIcon")
        descending = UIManager.getLookAndFeelDefaults().getIcon("Table.descendingSortIcon")
      }
      r1 -> {
        ascending = IconUIResource(EMPTY_ICON)
        descending = IconUIResource(EMPTY_ICON)
      }
      else -> {
        ascending = IconUIResource(ImageIcon(ascendingPath))
        descending = IconUIResource(ImageIcon(descendingPath))
      }
    }
    UIManager.put("Table.ascendingSortIcon", ascending)
    UIManager.put("Table.descendingSortIcon", descending)
    table.tableHeader.repaint()
  }
  val box1 = Box.createHorizontalBox()
  box1.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
  val bg = ButtonGroup()
  box1.add(JLabel("Table Sort Icon: "))
  listOf(r0, r1, r2).forEach { rb ->
    box1.add(rb)
    box1.add(Box.createHorizontalStrut(5))
    bg.add(rb)
    rb.addActionListener(al)
  }
  box1.add(Box.createHorizontalGlue())
  return box1
}

private class EmptyIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int
  ) {
    /* Empty icon */
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
