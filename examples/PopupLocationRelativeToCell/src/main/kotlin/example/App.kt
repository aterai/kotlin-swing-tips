package example

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*

fun makeUI(): Component {
  val tabs = JTabbedPane()
  tabs.addTab("JTable", makeTablePanel())
  tabs.addTab("JTree", makeTreePanel())
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTablePanel(): JPanel {
  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(initTable(JTable(5, 5))))
  p.add(JScrollPane(initTable(PopupLocationTable(5, 5))))
  return p
}

private fun initTable(table: JTable): JTable {
  table.setCellSelectionEnabled(true)
  table.setAutoCreateRowSorter(true)
  table.setFillsViewportHeight(true)
  table.setComponentPopupMenu(TablePopupMenu())
  return table
}

private fun makeTreePanel(): JPanel {
  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(initTree(JTree())))
  p.add(JScrollPane(initTree(PopupLocationTree())))
  return p
}

private fun initTree(tree: JTree): JTree {
  tree.setEditable(true)
  val popup = JPopupMenu()
  popup.add("clearSelection()").addActionListener { tree.clearSelection() }
  popup.addSeparator()
  popup.add("JMenuItem1")
  popup.add("JMenuItem2")
  tree.setComponentPopupMenu(popup)
  return tree
}

private class PopupLocationTable(
  numRows: Int,
  numColumns: Int,
) : JTable(numRows, numColumns) {
  private val leadSelectionCellRect: Rectangle
    get() {
      val row = getSelectionModel().leadSelectionIndex
      val col = getColumnModel().selectionModel.leadSelectionIndex
      return getCellRect(row, col, false)
    }

  override fun getPopupLocation(e: MouseEvent?): Point? {
    val r = this.leadSelectionCellRect
    val b = e == null && !r.isEmpty
    return if (b) getKeyPopupLocation(r) else super.getPopupLocation(e)
  }

  override fun editCellAt(row: Int, column: Int, e: EventObject?) =
    !isIgnoreKeys(e) && super.editCellAt(row, column, e)

  private fun getKeyPopupLocation(r: Rectangle): Point {
    val px = if (getCellSelectionEnabled()) r.maxX else bounds.centerX
    return Point(px.toInt(), r.maxY.toInt())
  }

  companion object {
    private val IGNORE_KEYS = listOf<Int>(
      KeyEvent.VK_F1,
      KeyEvent.VK_F2,
      KeyEvent.VK_F3,
      KeyEvent.VK_F4,
      KeyEvent.VK_F5,
      KeyEvent.VK_F6,
      KeyEvent.VK_F7,
      KeyEvent.VK_F8,
      KeyEvent.VK_F9,
      KeyEvent.VK_F10,
      KeyEvent.VK_F11,
      KeyEvent.VK_F12,
      KeyEvent.VK_F13,
      KeyEvent.VK_F14,
      KeyEvent.VK_F15,
      KeyEvent.VK_F16,
      KeyEvent.VK_F17,
      KeyEvent.VK_F18,
      KeyEvent.VK_F19,
      KeyEvent.VK_F20,
      KeyEvent.VK_F21,
      KeyEvent.VK_F22,
      KeyEvent.VK_F23,
      KeyEvent.VK_CONTEXT_MENU,
    )

    private fun isIgnoreKeys(e: EventObject?): Boolean =
      e is KeyEvent &&
        IGNORE_KEYS.contains(
          e.getKeyCode(),
        )
  }
}

private class TablePopupMenu : JPopupMenu() {
  init {
    val check = JCheckBoxMenuItem("setCellSelectionEnabled", true)
    add(check).addActionListener { e ->
      val b = (e.source as? JCheckBoxMenuItem)?.isSelected == true
      (getInvoker() as? JTable)?.also {
        it.setCellSelectionEnabled(b)
        it.setRowSelectionAllowed(true)
      }
    }
    add("clearSelectionAndLeadAnchor").addActionListener {
      (getInvoker() as? JTable)?.also {
        clearSelectionAndLeadAnchor(it)
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTable) {
      super.show(c, x, y)
    }
  }

  private fun clearSelectionAndLeadAnchor(table: JTable) {
    val selectionModel = table.getSelectionModel()
    val colSelectionModel = table.getColumnModel().selectionModel
    selectionModel.valueIsAdjusting = true
    colSelectionModel.valueIsAdjusting = true
    table.clearSelection()
    selectionModel.anchorSelectionIndex = -1
    selectionModel.leadSelectionIndex = -1
    colSelectionModel.anchorSelectionIndex = -1
    colSelectionModel.leadSelectionIndex = -1
    selectionModel.valueIsAdjusting = false
    colSelectionModel.valueIsAdjusting = false
  }
}

private class PopupLocationTree : JTree() {
  override fun getPopupLocation(e: MouseEvent?): Point? {
    val r = getRowBounds(getLeadSelectionRow())
    return if (e == null && r != null) {
      getKeyPopupLocation(r)
    } else {
      super.getPopupLocation(e)
    }
  }

  private fun getKeyPopupLocation(r: Rectangle) = Point(r.minX.toInt(), r.maxY.toInt())
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
