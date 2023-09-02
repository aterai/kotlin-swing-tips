package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Objects
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("RowHeader", "Description")
  val d0 = """
    0000
    1
    2
    3
    4
    5
    6
    7
    8
    9
    10
  """.trimIndent()
  val d1 = "1111111"
  val d2 = """
    2222222222222
    1
    2
    3
  """.trimIndent()
  val data = arrayOf(
    arrayOf(RowHeader("aaa", true, false), d0),
    arrayOf(RowHeader("bbb", false, false), d1),
    arrayOf(RowHeader("ccc", true, false), d2)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = column == 0
  }
  val defaultHeight = 20
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      autoCreateRowSorter = true
      surrendersFocusOnKeystroke = true
      setRowHeight(defaultHeight)
      setDefaultRenderer(RowHeader::class.java, RowHeaderRenderer())
      setDefaultEditor(RowHeader::class.java, RowHeaderEditor())
      val column = getColumnModel().getColumn(1)
      column.cellRenderer = TextAreaCellRenderer()
      column.preferredWidth = 160
    }
  }
  table.model.addTableModelListener { e ->
    val mr = e.firstRow
    val mc = e.column
    val vr = table.convertRowIndexToView(mr)
    val vc = table.convertColumnIndexToView(mc)
    val o = table.getValueAt(vr, vc)
    if (mc == 0 && o is RowHeader) {
      val vc1 = table.convertColumnIndexToView(1)
      val r = table.columnModel.getColumn(vc1).cellRenderer
      val v = table.getValueAt(vr, vc1)
      val c = r.getTableCellRendererComponent(table, v, true, true, vr, vc1)
      val h = if (o.isSelected) c.preferredSize.height else defaultHeight
      table.setRowHeight(vr, h)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextAreaCellRenderer : TableCellRenderer {
  private val textArea = JTextArea()

  init {
    textArea.lineWrap = true
    textArea.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    textArea.font = table.font
    textArea.text = value?.toString() ?: ""
    textArea.setSize(table.getCellRect(row, column, true).width, 0)
    return textArea
  }
}

private data class RowHeader(
  val title: String,
  val isExpandable: Boolean,
  val isSelected: Boolean
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is RowHeader) {
      return false
    }
    val rh = other
    return isExpandable == rh.isExpandable && isSelected == rh.isSelected && title == rh.title
  }

  override fun hashCode() = Objects.hash(title, isExpandable, isSelected)
}

private class RowHeaderPanel : JPanel(BorderLayout()) {
  val label = JLabel(" ")
  val check = JCheckBox()

  init {
    label.border = BorderFactory.createEmptyBorder(2, 4, 2, 0)
    check.isOpaque = false
    check.font = check.font.deriveFont(8f)
    check.icon = CheckIcon()
    val box = Box.createHorizontalBox().also {
      it.add(label)
      it.add(Box.createHorizontalGlue())
      it.add(check)
    }
    add(box, BorderLayout.NORTH)
  }

  override fun isOpaque() = true
}

private class RowHeaderRenderer : TableCellRenderer {
  private val renderer = RowHeaderPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (value is RowHeader) {
      renderer.check.isVisible = value.isExpandable
      renderer.check.isSelected = value.isSelected
      renderer.label.text = value.title
    }
    return renderer
  }
}

private class RowHeaderEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = RowHeaderPanel()
  private var rowHeader: RowHeader? = null

  init {
    renderer.check.addActionListener {
      rowHeader = rowHeader?.let {
        RowHeader(it.title, it.isExpandable, renderer.check.isSelected)
      }
      fireEditingStopped()
    }
    renderer.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        fireEditingStopped()
      }
    })
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (value is RowHeader) {
      renderer.check.isVisible = value.isExpandable
      renderer.label.text = value.title
      val b = value.isExpandable && renderer.check.isSelected
      rowHeader = RowHeader(value.title, value.isExpandable, b)
    }
    return renderer
  }

  override fun getCellEditorValue() = rowHeader
}

private class CheckIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.font = c.font
    if (c is AbstractButton) {
      val txt = if (c.isSelected) "Å»" else "Å…"
      g2.drawString(txt, x, y + 10)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
