package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
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
    arrayOf("DDD", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table0 = JTable(model)
  table0.setAutoCreateRowSorter(true)
  table0.putClientProperty("terminateEditOnFocusLost", true)

  val table1 = makeTable(model)
  table1.setAutoCreateRowSorter(true)
  table1.putClientProperty("terminateEditOnFocusLost", true)

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.setTopComponent(JScrollPane(table0))
  sp.setBottomComponent(JScrollPane(table1))
  sp.setResizeWeight(.5)
  sp.setPreferredSize(Dimension(320, 240))
  return sp
}

private fun makeTable(model: TableModel): JTable {
  return object : JTable(model) {
    override fun updateUI() {
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      super.updateUI()
      updateRenderer()
      val checkBox = makeBooleanEditor(this)
      setDefaultEditor(java.lang.Boolean::class.java, DefaultCellEditor(checkBox))
    }

    private fun updateRenderer() {
      val m = getModel()
      for (i in 0 until m.getColumnCount()) {
        (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
          SwingUtilities.updateComponentTreeUI(it)
        }
      }
    }

    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
      val c = super.prepareEditor(editor, row, column)
      if (c is JCheckBox) {
        c.setBackground(getSelectionBackground())
        c.setBorderPainted(true)
      }
      return c
    }
  }
}

private fun makeBooleanEditor(table: JTable): JCheckBox {
  val checkBox = JCheckBox()
  checkBox.setHorizontalAlignment(SwingConstants.CENTER)
  checkBox.setBorderPainted(true)
  checkBox.setOpaque(true)
  checkBox.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      (e.getComponent() as? JCheckBox)?.also { cb ->
        val m = cb.getModel()
        val editingRow = table.getEditingRow()
        if (m.isPressed() && table.isRowSelected(editingRow) && e.isControlDown()) {
          if (editingRow % 2 == 0) {
            cb.setOpaque(false)
          } else {
            cb.setOpaque(true)
            cb.setBackground(UIManager.getColor("Table.alternateRowColor"))
          }
        } else {
          cb.setBackground(table.getSelectionBackground())
          cb.setOpaque(true)
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
        table.getCellEditor().cancelCellEditing()
      }
    }
  })
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
