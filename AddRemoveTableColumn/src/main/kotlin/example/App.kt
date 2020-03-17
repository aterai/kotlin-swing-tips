package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumnModel

class MainPanel : JPanel(BorderLayout()) {
  init {
    UIManager.put("CheckBoxMenuItem.doNotCloseOnMouseClick", true)
    val table = JTable(DefaultTableModel(12, 8))
    table.getTableHeader().setComponentPopupMenu(TableHeaderPopupMenu(table))
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

class TableHeaderPopupMenu(table: JTable) : JPopupMenu() {
  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTableHeader)?.also {
      it.setDraggedColumn(null)
      it.repaint()
      it.getTable().repaint()
      updateMenuItems(it.getColumnModel())
      super.show(c, x, y)
    }
  }

  private fun updateMenuItems(columnModel: TableColumnModel) {
    val isOnlyOneMenu = columnModel.getColumnCount() == 1
    if (isOnlyOneMenu) {
      descendants(this)
        .map { it.getComponent() }
        .forEach { it.setEnabled(it !is AbstractButton || !it.isSelected()) }
    } else {
      descendants(this)
        .forEach { it.getComponent().setEnabled(true) }
    }
  }

  private fun descendants(me: MenuElement): List<MenuElement> =
    // me.getSubElements().map { children(it) }.fold(listOf(me)) { a, b -> a + b }
    me.getSubElements().flatMap { listOf(it) + descendants(it) }

  init {
    val columnModel = table.getColumnModel()
    columnModel.getColumns().toList().forEach { tableColumn ->
      val name = tableColumn.getHeaderValue()?.toString() ?: ""
      val item = JCheckBoxMenuItem(name, true)
      item.addItemListener { e ->
        val check = e.getItemSelectable() as? AbstractButton ?: return@addItemListener
        if (check.isSelected()) {
          columnModel.addColumn(tableColumn)
        } else {
          columnModel.removeColumn(tableColumn)
        }
        updateMenuItems(columnModel)
      }
      add(item)
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
