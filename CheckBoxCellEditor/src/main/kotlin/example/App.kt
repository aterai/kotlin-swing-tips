package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("String", "Boolean")
    val data = arrayOf(
      arrayOf("AAA", true),
      arrayOf("bbb", false),
      arrayOf("CCC", true),
      arrayOf("ddd", false),
      arrayOf("EEE", true),
      arrayOf("fff", false)
    )
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) =
        if (column == 1) java.lang.Boolean::class.java else String::class.java
        // not work: if (column == 1) Boolean::class.java else String::class.java

      override fun isCellEditable(row: Int, column: Int) = column == 1
    }
    val table = object : JTable(model) {
      override fun updateUI() {
        setDefaultEditor(java.lang.Boolean::class.java, null)
        super.updateUI()
        setDefaultEditor(java.lang.Boolean::class.java, CheckBoxPanelEditor())
      }
    }
    table.putClientProperty("terminateEditOnFocusLost", true)
    table.setRowHeight(24)
    table.setRowSelectionAllowed(true)
    table.setShowVerticalLines(false)
    table.setIntercellSpacing(Dimension(0, 1))
    table.setFocusable(false)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

class CheckBoxPanelEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = object : JPanel(GridBagLayout()) {
    @Transient
    private var listener: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(listener)
      super.updateUI()
      setBorder(UIManager.getBorder("Table.noFocusBorder"))
      listener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          fireEditingStopped()
        }
      }
      addMouseListener(listener)
    }
  }
  private val checkBox = object : JCheckBox() {
    @Transient
    private var handler: CellEditorHandler? = null

    override fun updateUI() {
      removeActionListener(handler)
      removeMouseListener(handler)
      super.updateUI()
      setOpaque(false)
      setFocusable(false)
      setRolloverEnabled(false)
      handler = CellEditorHandler()
      addActionListener(handler)
      addMouseListener(handler)
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    checkBox.setSelected(value == true)
    renderer.add(checkBox)
    return renderer
  }

  override fun getCellEditorValue() = checkBox.isSelected()

  private inner class CellEditorHandler : MouseAdapter(), ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      fireEditingStopped()
    }

    override fun mousePressed(e: MouseEvent) {
      (SwingUtilities.getAncestorOfClass(JTable::class.java, e.getComponent()) as? JTable)?.also {
        if (checkBox.getModel().isPressed() && it.isRowSelected(it.getEditingRow()) && e.isControlDown()) {
          renderer.setBackground(it.getBackground())
        } else {
          renderer.setBackground(it.getSelectionBackground())
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      (SwingUtilities.getAncestorOfClass(JTable::class.java, e.getComponent()) as? JTable)
        ?.takeIf { it.isEditing() }
        ?.also { it.removeEditor() }
    }
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
