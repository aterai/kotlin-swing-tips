package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

class MainPanel : JPanel(GridBagLayout()) {
  init {
    val columnNames = arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I")
    val data = arrayOf(
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true),
        arrayOf(true, false, true, false, true, true, false, true, true))

    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int): Class<*> = java.lang.Boolean::class.java
    }

    val table = object : JTable(model) {
      override fun getPreferredScrollableViewportSize() = super.getPreferredSize()

      override fun updateUI() {
        setDefaultEditor(java.lang.Boolean::class.java, null)
        super.updateUI()
        setDefaultEditor(java.lang.Boolean::class.java, BooleanEditor())
      }
    }

    table.setCellSelectionEnabled(true)
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    table.setAutoCreateRowSorter(true)
    table.setComponentPopupMenu(TablePopupMenu())

    val m = table.getColumnModel()
    (0 until m.getColumnCount()).forEach {
      val col = m.getColumn(it)
      col.setPreferredWidth(CELLSIZE)
      col.setResizable(false)
    }

    val scroll = JScrollPane(table)
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

    add(scroll)
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    val CELLSIZE = 24
  }
}

internal class TablePopupMenu : JPopupMenu() {
  private val select: JMenuItem
  private val clear: JMenuItem
  private val toggle: JMenuItem

  init {
    select = add("select")
    select.addActionListener {
      val table = getInvoker() as JTable
      for (row in table.getSelectedRows()) {
        for (col in table.getSelectedColumns()) {
          table.setValueAt(true, row, col)
        }
      }
    }

    clear = add("clear")
    clear.addActionListener {
      val table = getInvoker() as JTable
      for (row in table.getSelectedRows()) {
        for (col in table.getSelectedColumns()) {
          table.setValueAt(false, row, col)
        }
      }
    }

    toggle = add("toggle")
    toggle.addActionListener {
      val table = getInvoker() as JTable
      for (row in table.getSelectedRows()) {
        for (col in table.getSelectedColumns()) {
          var b = table.getValueAt(row, col) as Boolean
          table.setValueAt(!b, row, col)
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      val isSelected = c.getSelectedRowCount() > 0
      select.setEnabled(isSelected)
      clear.setEnabled(isSelected)
      toggle.setEnabled(isSelected)
      super.show(c, x, y)
    }
  }
}

internal class BooleanEditor : AbstractCellEditor(), TableCellEditor {
  protected val renderer: Container = object : JPanel(GridBagLayout()) {
    protected var listener: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(listener)
      super.updateUI()
      listener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          fireEditingStopped()
        }
      }
      addMouseListener(listener)
    }
  }
  protected val checkBox: JCheckBox = object : JCheckBox() {
    var handler: CheckBoxHandler? = null

    override fun updateUI() {
      removeActionListener(handler)
      removeMouseListener(handler)
      super.updateUI()
      setBorder(UIManager.getBorder("Table.noFocusBorder"))
      setOpaque(false)
      setFocusable(false)
      setRolloverEnabled(false)
      handler = CheckBoxHandler()
      addActionListener(handler)
      addMouseListener(handler)
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    checkBox.setSelected(value == java.lang.Boolean.TRUE)
    renderer.add(checkBox)
    return renderer
  }

  override fun getCellEditorValue(): Any = checkBox.isSelected()

  override fun isCellEditable(e: EventObject): Boolean {
    if (e is MouseEvent) {
      return !(e.isShiftDown() || e.isControlDown())
    }
    return super.isCellEditable(e)
  }

  private inner class CheckBoxHandler : MouseAdapter(), ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      fireEditingStopped()
    }

    override fun mousePressed(e: MouseEvent) {
      val c = SwingUtilities.getAncestorOfClass(JTable::class.java, e.getComponent())
      if (c is JTable) {
        if (checkBox.getModel().isPressed() && c.isRowSelected(c.getEditingRow()) && e.isControlDown()) {
          renderer.setBackground(c.getBackground())
        } else {
          renderer.setBackground(c.getSelectionBackground())
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      val clz = JTable::class.java
      SwingUtilities.getAncestorOfClass(clz, e.getComponent())
          ?.takeIf { clz.isInstance(it) }
          ?.let { clz.cast(it) }
          ?.takeIf { table -> table.isEditing() }
          ?.let { table -> table.removeEditor() }
    }
  }
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
