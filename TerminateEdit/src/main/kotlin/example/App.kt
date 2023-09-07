package example

import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private fun makeTable(): JTable {
  val columnNames = arrayOf("String", "Integer")
  val data = arrayOf(
    arrayOf("aaa", 12),
    arrayOf("bbb", 5),
    arrayOf("CCC", 92),
    arrayOf("DDD", 0)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (isRowSelected(row)) {
        c.foreground = getSelectionForeground()
        c.background = getSelectionBackground()
      } else {
        c.foreground = foreground
        c.background = if (row % 2 == 0) evenColor else background
      }
      return c
    }
  }
  table.autoCreateRowSorter = true
  table.putClientProperty("terminateEditOnFocusLost", true)
  return table
}

fun makeUI(): Component {
  val table = makeTable()
  val focusCheck = JCheckBox("DefaultCellEditor:focusLost", true)
  (table.getDefaultEditor(Object::class.java) as? DefaultCellEditor)?.also {
    val fl = object : FocusAdapter() {
      override fun focusLost(e: FocusEvent) {
        if (!focusCheck.isSelected) {
          return
        }
        if (table.isEditing) {
          table.cellEditor.stopCellEditing()
        }
      }
    }
    it.component.addFocusListener(fl)
  }
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val headerCheck = JCheckBox("TableHeader:mousePressed", true)
  val ml = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (!headerCheck.isSelected) {
        return
      }
      if (table.isEditing) {
        table.cellEditor.stopCellEditing()
      }
    }
  }
  table.tableHeader.addMouseListener(ml)

  val comboBox = JComboBox(AutoResizeMode.values())
  comboBox.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is AutoResizeMode) {
      table.autoResizeMode = item.autoResizeMode
    }
  }

  val key = "terminateEditOnFocusLost"
  val tefCheck = JCheckBox(key, true)
  tefCheck.addActionListener { e ->
    val isSelected = (e.source as? JCheckBox)?.isSelected == true
    table.putClientProperty(key, isSelected)
  }
  val box = JPanel(GridLayout(4, 0))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(tefCheck)
  box.add(focusCheck)
  box.add(headerCheck)
  box.add(comboBox)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class AutoResizeMode(val autoResizeMode: Int) {
  AUTO_RESIZE_OFF(JTable.AUTO_RESIZE_OFF),
  AUTO_RESIZE_ALL_COLUMNS(JTable.AUTO_RESIZE_ALL_COLUMNS)
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
