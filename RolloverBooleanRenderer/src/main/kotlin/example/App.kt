package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf("String", "Integer", "Boolean")
  private val data = arrayOf(
      arrayOf<Any>("aaa", 12, true),
      arrayOf<Any>("bbb", 5, false),
      arrayOf<Any>("CCC", 92, true),
      arrayOf<Any>("DDD", 0, false))
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  private val table = object : JTable(model) {
    @Transient
    protected var highlighter: HighlightListener? = null

    override fun updateUI() {
      addMouseListener(highlighter)
      addMouseMotionListener(highlighter)
      setDefaultRenderer(Any::class.java, null)
      setDefaultRenderer(Number::class.java, null)
      setDefaultRenderer(java.lang.Boolean::class.java, null)
      super.updateUI()
      val hl = HighlightListener()
      highlighter = hl
      addMouseListener(hl)
      addMouseMotionListener(hl)
      setDefaultRenderer(Any::class.java, RolloverDefaultTableCellRenderer(hl))
      setDefaultRenderer(Number::class.java, RolloverNumberRenderer(hl))
      setDefaultRenderer(java.lang.Boolean::class.java, RolloverBooleanRenderer(hl))
    }

    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int) =
        super.prepareEditor(editor, row, column).also {
          (it as? JCheckBox)?.setBackground(getSelectionBackground())
        }
  }

  init {
    table.setAutoCreateRowSorter(true)

    val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    sp.setTopComponent(JScrollPane(JTable(model)))
    sp.setBottomComponent(JScrollPane(table))
    sp.setResizeWeight(.5)

    add(sp)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class HighlightListener : MouseAdapter() {
  private var vrow = -1 // viewRowIndex
  private var vcol = -1 // viewColumnIndex

  fun isHighlightableCell(row: Int, column: Int) = vrow == row && vcol == column

  override fun mouseMoved(e: MouseEvent) {
    (e.getComponent() as? JTable)?.also { table ->
      val pt = e.getPoint()
      val prevRow = vrow
      val prevCol = vcol
      vrow = table.rowAtPoint(pt)
      vcol = table.columnAtPoint(pt)
      if (vrow < 0 || vcol < 0) {
        vrow = -1
        vcol = -1
      }
      if (vrow == prevRow && vcol == prevCol) {
        return
      }
      val repaintRect = if (vrow >= 0 && vcol >= 0) {
        val r = table.getCellRect(vrow, vcol, false)
        if (prevRow >= 0 && prevCol >= 0) r.union(table.getCellRect(prevRow, prevCol, false)) else r
      } else {
        table.getCellRect(prevRow, prevCol, false)
      }
      table.repaint(repaintRect)
    }
  }

  override fun mouseExited(e: MouseEvent) {
    (e.getComponent() as? JTable)?.also { table ->
      if (vrow >= 0 && vcol >= 0) {
        table.repaint(table.getCellRect(vrow, vcol, false))
      }
      vrow = -1
      vcol = -1
    }
  }
}

internal open class RolloverDefaultTableCellRenderer(val highlighter: HighlightListener) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    val str = value?.toString() ?: ""
//     if (highlighter.isHighlightableCell(row, column)) {
//       setText("<html><u>$str")
//       setForeground(if (isSelected) table.getSelectionForeground() else HIGHLIGHT)
//       setBackground(if (isSelected) table.getSelectionBackground().darker() else table.getBackground())
//     } else {
//       setText(str)
//       setForeground(if (isSelected) table.getSelectionForeground() else table.getForeground())
//       setBackground(if (isSelected) table.getSelectionBackground() else table.getBackground())
//     }
    val isHighlightableCell = highlighter.isHighlightableCell(row, column)
    setForeground(when {
      isSelected -> table.getSelectionForeground()
      !isSelected && isHighlightableCell -> HIGHLIGHT
      else -> table.getForeground()
    })
    setBackground(when {
      isSelected && isHighlightableCell -> table.getSelectionBackground().darker()
      isSelected && !isHighlightableCell -> table.getSelectionBackground()
      else -> table.getBackground()
    })
    setText(if (isHighlightableCell) "<html><u>$str" else str)
    return this
  }

  companion object {
    private val HIGHLIGHT = Color(255, 150, 50)
  }
}

internal class RolloverNumberRenderer(highlighter: HighlightListener) : RolloverDefaultTableCellRenderer(highlighter) {
  init {
    setHorizontalAlignment(SwingConstants.RIGHT)
  }
}

internal class RolloverBooleanRenderer(val highlighter: HighlightListener) : JCheckBox(), TableCellRenderer {
  init {
    setHorizontalAlignment(SwingConstants.CENTER)
    setBorderPainted(true)
    setRolloverEnabled(true)
    setOpaque(true)
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    getModel().setRollover(highlighter.isHighlightableCell(row, column))

    if (isSelected) {
      setForeground(table.getSelectionForeground())
      super.setBackground(table.getSelectionBackground())
    } else {
      setForeground(table.getForeground())
      setBackground(table.getBackground())
      // setBackground(row % 2 == 0 ? table.getBackground() : Color.WHITE); // Nimbus
    }
    setSelected(value == true)
    return this
  }

  // Overridden for performance reasons. ---->
  override fun isOpaque(): Boolean {
    val o = SwingUtilities.getAncestorOfClass(JTable::class.java, this)
    return (o as? JTable)?.let { table ->
      val back = getBackground()
      val colorMatch = back != null && back == table.getBackground() && table.isOpaque()
      !colorMatch && super.isOpaque()
    } ?: super.isOpaque()
  }

  protected override fun firePropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {}
  //   System.out.println(propertyName);
  //   if (propertyName == "border" ||
  //       ((propertyName == "font" || propertyName == "foreground") && oldValue != newValue)) {
  //     super.firePropertyChange(propertyName, oldValue, newValue);
  //   }
  // }

  override fun firePropertyChange(propertyName: String, oldValue: Boolean, newValue: Boolean) {}

  override fun repaint(tm: Long, x: Int, y: Int, width: Int, height: Int) {}

  override fun repaint(r: Rectangle) {}

  override fun repaint() {}

  override fun invalidate() {}

  override fun validate() {}

  override fun revalidate() {}
  // <---- Overridden for performance reasons.
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
