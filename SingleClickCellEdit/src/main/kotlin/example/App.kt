package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf(
    arrayOf("aaa", "ee ee", "l"),
    arrayOf("bbb", "ff", "ggg"),
    arrayOf("CCC", "kkk", "jj"),
    arrayOf("DDD", "ii", "hhh")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.rowSelectionAllowed = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  table.intercellSpacing = Dimension()
  table.setShowGrid(false)
  table.putClientProperty("terminateEditOnFocusLost", true)

  val tableHeader = table.tableHeader
  tableHeader.reorderingAllowed = false

  val defaultRenderer = table.getDefaultRenderer(Any::class.java)
  val underlineRenderer = UnderlineCellRenderer()
  val modelCheck = JCheckBox("edit the cell on single click")
  modelCheck.addActionListener {
    (table.getDefaultEditor(Any::class.java) as? DefaultCellEditor)?.also {
      if (modelCheck.isSelected) {
        table.setDefaultRenderer(Any::class.java, underlineRenderer)
        table.addMouseListener(underlineRenderer)
        table.addMouseMotionListener(underlineRenderer)
        it.clickCountToStart = 1
      } else {
        table.setDefaultRenderer(Any::class.java, defaultRenderer)
        table.removeMouseListener(underlineRenderer)
        table.removeMouseMotionListener(underlineRenderer)
        it.clickCountToStart = 2
      }
    }
  }

  val scrollPane = JScrollPane(table)
  scrollPane.viewport.background = Color.WHITE

  return JPanel(BorderLayout()).also {
    it.add(modelCheck, BorderLayout.NORTH)
    it.add(scrollPane)
    it.preferredSize = Dimension(320, 240)
  }
}

class UnderlineCellRenderer : DefaultTableCellRenderer(), MouseListener, MouseMotionListener {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (c is JLabel) {
      val str = value?.toString() ?: ""
      val b = !table.isEditing && viewRowIndex == row && viewColumnIndex == column
      c.text = if (b) "<html><u>$str" else str
    }
    return c
  }

  override fun mouseMoved(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      val pt = e.point
      viewRowIndex = table.rowAtPoint(pt)
      viewColumnIndex = table.columnAtPoint(pt)
      if (viewRowIndex < 0 || viewColumnIndex < 0) {
        viewRowIndex = -1
        viewColumnIndex = -1
      }
      table.repaint()
    }
  }

  override fun mouseExited(e: MouseEvent) {
    viewRowIndex = -1
    viewColumnIndex = -1
    e.component.repaint()
  }

  override fun mouseDragged(e: MouseEvent) {
    // not needed
  }

  override fun mouseClicked(e: MouseEvent) {
    // not needed
  }

  override fun mouseEntered(e: MouseEvent) {
    // not needed
  }

  override fun mousePressed(e: MouseEvent) {
    // not needed
  }

  override fun mouseReleased(e: MouseEvent) {
    // not needed
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
