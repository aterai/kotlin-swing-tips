package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table0 = JTable(model)
  table0.autoCreateRowSorter = true
  table0.putClientProperty("terminateEditOnFocusLost", true)

  val table1 = makeTable(model)
  table1.autoCreateRowSorter = true
  table1.putClientProperty("terminateEditOnFocusLost", true)

  return JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(table0), JScrollPane(table1)).also {
    it.resizeWeight = .5
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(model: TableModel) = object : JTable(model) {
  override fun updateUI() {
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    updateRenderer()
    val checkBox = makeBooleanEditor(this)
    setDefaultEditor(Boolean::class.javaObjectType, DefaultCellEditor(checkBox))
  }

  private fun updateRenderer() {
    val m = getModel()
    for (i in 0 until m.columnCount) {
      (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
        SwingUtilities.updateComponentTreeUI(it)
      }
    }
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareEditor(editor, row, column)
    c.background = getSelectionBackground()
    (c as? JCheckBox)?.isBorderPainted = true
    return c
  }
}

private fun makeBooleanEditor(table: JTable): JCheckBox {
  val checkBox = JCheckBox()
  checkBox.horizontalAlignment = SwingConstants.CENTER
  checkBox.isBorderPainted = true
  checkBox.isOpaque = true
  val ml = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      (e.component as? JCheckBox)?.also { cb ->
        val m = cb.model
        val editingRow = table.editingRow
        if (m.isPressed && table.isRowSelected(editingRow) && e.isControlDown) {
          if (editingRow % 2 == 0) {
            cb.isOpaque = false
          } else {
            cb.isOpaque = true
            cb.background = UIManager.getColor("Table.alternateRowColor")
          }
        } else {
          cb.background = table.selectionBackground
          cb.isOpaque = true
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      if (table.isEditing && !table.cellEditor.stopCellEditing()) {
        table.cellEditor.cancelCellEditing()
      }
    }
  }
  checkBox.addMouseListener(ml)
  return checkBox
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
