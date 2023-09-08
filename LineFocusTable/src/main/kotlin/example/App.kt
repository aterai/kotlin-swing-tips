package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EnumSet
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

private val columnNames = arrayOf("String", "Integer", "Boolean")
private val data = arrayOf(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = when (column) {
    0 -> String::class.java
    1 -> Number::class.java
    2 -> Boolean::class.javaObjectType
    else -> super.getColumnClass(column)
  }
}
private val table = LineFocusTable(model)

fun makeUI(): Component {
  UIManager.put("Table.focusCellHighlightBorder", DotBorder(2, 2, 2, 2))

  table.rowSelectionAllowed = true
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.setShowGrid(false)
  table.intercellSpacing = Dimension()
  table.putClientProperty("terminateEditOnFocusLost", true)

  table.componentPopupMenu = TablePopupMenu()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class LineFocusTable(model: TableModel) : JTable(model) {
  private val dotBorder = DotBorder(2, 2, 2, 2)
  private val emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  override fun updateUI() {
    // Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
    // https://bugs.openjdk.org/browse/JDK-6788475
    // Set a temporary ColorUIResource to avoid this issue
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    updateRenderer()
    remakeBooleanEditor()
  }

  private fun updateRenderer() {
    val m = model
    for (i in 0 until m.columnCount) {
      (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
        SwingUtilities.updateComponentTreeUI(it)
      }
    }
  }

  private fun remakeBooleanEditor() {
    val checkBox = JCheckBox()
    checkBox.horizontalAlignment = SwingConstants.CENTER
    checkBox.isBorderPainted = true
    checkBox.isOpaque = true
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        val cb = e.component as? JCheckBox ?: return
        val m = cb.model
        if (m.isPressed && isRowSelected(getEditingRow()) && e.isControlDown) {
          if (getEditingRow() % 2 == 0) {
            cb.isOpaque = false
            // cb.setBackground(getBackground())
          } else {
            cb.isOpaque = true
            cb.background = UIManager.getColor("Table.alternateRowColor")
          }
        } else {
          cb.background = getSelectionBackground()
          cb.isOpaque = true
        }
      }

      override fun mouseExited(e: MouseEvent) {
        // in order to drag table row selection
        if (isEditing && !getCellEditor().stopCellEditing()) {
          getCellEditor().cancelCellEditing()
        }
      }
    }
    checkBox.addMouseListener(ml)
    setDefaultEditor(Boolean::class.javaObjectType, DefaultCellEditor(checkBox))
  }

  private fun updateBorderType(border: DotBorder, column: Int) {
    border.type.clear() // = EnumSet.noneOf(Type.class)
    if (column == 0) {
      border.type.add(Type.START)
    }
    if (column == columnCount - 1) {
      border.type.add(Type.END)
    }
  }

  override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
    val o = super.prepareRenderer(tcr, row, column)
    val c = o as? JComponent ?: return o
    (c as? JCheckBox)?.isBorderPainted = true
    if (row == getSelectionModel().leadSelectionIndex) { // isRowSelected(row)) {
      c.border = dotBorder
      updateBorderType(dotBorder, column)
    } else {
      c.border = emptyBorder
    }
    return c
  }

  override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
    val c = super.prepareEditor(editor, row, column)
    (c as? JCheckBox)?.also {
      it.border = dotBorder
      updateBorderType(dotBorder, column)
      // updateBorderType((DotBorder) it.getBorder(), column)
      // it.setBorderPainted(true)
      // it.setBackground(getSelectionBackground())
    }
    return c
  }
}

private enum class Type {
  START,
  END
}

private class DotBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int
) : EmptyBorder(top, left, bottom, right) {
  val type: MutableSet<Type> = EnumSet.noneOf(Type::class.java)

  override fun isBorderOpaque() = true

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = DOT_COLOR
    g2.stroke = DASHED
    if (type.contains(Type.START)) {
      g2.drawLine(0, 0, 0, h)
    }
    if (type.contains(Type.END)) {
      g2.drawLine(w - 1, 0, w - 1, h)
    }
    if (c.bounds.x % 2 == 0) {
      g2.drawLine(0, 0, w, 0)
      g2.drawLine(0, h - 1, w, h - 1)
    } else {
      g2.drawLine(1, 0, w, 0)
      g2.drawLine(1, h - 1, w, h - 1)
    }
    g2.dispose()
  }

  companion object {
    private val DASHED = BasicStroke(
      1f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,
      10f,
      floatArrayOf(1f),
      0f,
    )
    private val DOT_COLOR = Color(0xC8_96_96)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        model.addRow(arrayOf("New row", model.getRowCount(), false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    add("clearSelection").addActionListener { (invoker as? JTable)?.clearSelection() }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    (c as? JTable)?.also {
      delete.isEnabled = it.selectedRowCount > 0
      super.show(it, x, y)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
