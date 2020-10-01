package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumnModel

fun makeUI(): Component {
  UIManager.put("CheckBoxMenuItem.doNotCloseOnMouseClick", true)
  val table = JTable(DefaultTableModel(12, 8))
  table.tableHeader.componentPopupMenu = TableHeaderPopupMenu(table)
  return JScrollPane(table).also {
    it.preferredSize = Dimension(320, 240)
  }
}

private class TableHeaderPopupMenu(table: JTable) : JPopupMenu() {
  init {
    val columnModel = table.columnModel
    columnModel.columns.toList().forEach { tableColumn ->
      val name = tableColumn.headerValue?.toString() ?: ""
      val item = JCheckBoxMenuItem(name, true)
      item.addItemListener { e ->
        if ((e.itemSelectable as? AbstractButton)?.isSelected == true) {
          columnModel.addColumn(tableColumn)
        } else {
          columnModel.removeColumn(tableColumn)
        }
        updateMenuItems(columnModel)
      }
      add(item)
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTableHeader)?.also {
      it.draggedColumn = null
      it.repaint()
      it.table.repaint()
      updateMenuItems(it.columnModel)
      super.show(c, x, y)
    }
  }

  private fun updateMenuItems(columnModel: TableColumnModel) {
    val isOnlyOneMenu = columnModel.columnCount == 1
    if (isOnlyOneMenu) {
      descendants(this)
        .map { it.component }
        .forEach { it.isEnabled = it !is AbstractButton || !it.isSelected }
    } else {
      descendants(this)
        .forEach { it.component.isEnabled = true }
    }
  }

  private fun descendants(me: MenuElement): List<MenuElement> =
    me.subElements.flatMap { listOf(it) + descendants(it) }
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
