package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EnumSet
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.EmptyBorder
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf("String", "Integer", "Boolean")
  private val data = arrayOf(
      arrayOf<Any>("aaa", 12, true),
      arrayOf<Any>("bbb", 5, false),
      arrayOf<Any>("CCC", 92, true),
      arrayOf<Any>("DDD", 0, false))
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> java.lang.Boolean::class.java
      else -> super.getColumnClass(column)
    }
  }
  private val table = LineFocusTable(model)

  init {
    UIManager.put("Table.focusCellHighlightBorder", DotBorder(2, 2, 2, 2))

    table.setRowSelectionAllowed(true)
    table.setAutoCreateRowSorter(true)
    table.setFillsViewportHeight(true)
    table.setShowGrid(false)
    table.setIntercellSpacing(Dimension())
    table.putClientProperty("terminateEditOnFocusLost", true)

    table.setComponentPopupMenu(TablePopupMenu())
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

// class TestRenderer extends DefaultTableCellRenderer {
//   private static final DotBorder dotBorder = new DotBorder(2, 2, 2, 2);
//   private static final Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
//   @Override public Component getTableCellRendererComponent(
//         JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//     Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//     if (c instanceof JComponent) {
//       int lsi = table.getSelectionModel().getLeadSelectionIndex();
//       ((JComponent) c).setBorder(row == lsi ? dotBorder : emptyBorder);
//       dotBorder.setLastCellFlag(row == lsi && column == table.getColumnCount() - 1);
//     }
//     return c;
//   }
// }

class LineFocusTable(model: TableModel) : JTable(model) {
  private val dotBorder = DotBorder(2, 2, 2, 2)
  private val emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  override fun updateUI() {
    // [JDK-6788475] Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely - Java Bug System
    // https://bugs.openjdk.java.net/browse/JDK-6788475
    // XXX: set dummy ColorUIResource
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    updateRenderer()
    remakeBooleanEditor()
  }

  private fun updateRenderer() {
    val m = getModel()
    for (i in 0 until m.getColumnCount()) {
      (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
        SwingUtilities.updateComponentTreeUI(it)
      }
    }
  }

  private fun remakeBooleanEditor() {
    val checkBox = JCheckBox()
    checkBox.setHorizontalAlignment(SwingConstants.CENTER)
    checkBox.setBorderPainted(true)
    checkBox.setOpaque(true)
    checkBox.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        val cb = e.getComponent() as? JCheckBox ?: return
        val m = cb.getModel()
        if (m.isPressed() && isRowSelected(getEditingRow()) && e.isControlDown()) {
          if (getEditingRow() % 2 == 0) {
            cb.setOpaque(false)
            // cb.setBackground(getBackground());
          } else {
            cb.setOpaque(true)
            cb.setBackground(UIManager.getColor("Table.alternateRowColor"))
          }
        } else {
          cb.setBackground(getSelectionBackground())
          cb.setOpaque(true)
        }
      }

      override fun mouseExited(e: MouseEvent) {
        // in order to drag table row selection
        if (isEditing() && !getCellEditor().stopCellEditing()) {
          getCellEditor().cancelCellEditing()
        }
      }
    })
    setDefaultEditor(Boolean::class.java, DefaultCellEditor(checkBox))
  }

  private fun updateBorderType(border: DotBorder, column: Int) {
    border.type.clear() // = EnumSet.noneOf(Type.class);
    if (column == 0) {
      border.type.add(Type.START)
    }
    if (column == getColumnCount() - 1) {
      border.type.add(Type.END)
    }
  }

  override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
    val o = super.prepareRenderer(tcr, row, column)
    val c = o as? JComponent ?: return o
    (c as? JCheckBox)?.also {
      it.setBorderPainted(true)
    }
    if (row == getSelectionModel().getLeadSelectionIndex()) { // isRowSelected(row)) {
      c.setBorder(dotBorder)
      updateBorderType(dotBorder, column)
    } else {
      c.setBorder(emptyBorder)
    }
    return c
  }

  override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
    val c = super.prepareEditor(editor, row, column)
    (c as? JCheckBox)?.also {
      it.setBorder(dotBorder)
      updateBorderType(dotBorder, column)
      // updateBorderType((DotBorder) it.getBorder(), column)
      // it.setBorderPainted(true)
      // it.setBackground(getSelectionBackground())
    }
    return c
  }
}

enum class Type {
  START, END
}

class DotBorder(top: Int, left: Int, bottom: Int, right: Int) : EmptyBorder(top, left, bottom, right) {
  val type: MutableSet<Type> = EnumSet.noneOf(Type::class.java)

  override fun isBorderOpaque() = true

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(DOT_COLOR)
    g2.setStroke(DASHED)
    if (type.contains(Type.START)) {
      g2.drawLine(0, 0, 0, h)
    }
    if (type.contains(Type.END)) {
      g2.drawLine(w - 1, 0, w - 1, h)
    }
    if (c.getBounds().x % 2 == 0) {
      g2.drawLine(0, 0, w, 0)
      g2.drawLine(0, h - 1, w, h - 1)
    } else {
      g2.drawLine(1, 0, w, 0)
      g2.drawLine(1, h - 1, w, h - 1)
    }
    g2.dispose()
  }

  companion object {
    private val DASHED = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(1f), 0f)
    private val DOT_COLOR = Color(0xC8_96_96)
  }
}

class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = getInvoker() as JTable
      val model = table.getModel() as DefaultTableModel
      model.addRow(arrayOf<Any>("New row", model.getRowCount(), false))
      val r = table.getCellRect(model.getRowCount() - 1, 0, true)
      table.scrollRectToVisible(r)
    }
    add("clearSelection").addActionListener { (getInvoker() as? JTable)?.clearSelection() }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = getInvoker() as JTable
      val model = table.getModel() as DefaultTableModel
      val selection = table.getSelectedRows()
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTable)?.also {
      delete.setEnabled(it.getSelectedRowCount() > 0)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
