package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumnModel

class MainPanel : JPanel(BorderLayout()) {
  init {
    UIManager.put("CheckBoxMenuItem.doNotCloseOnMouseClick", true)
    val table = JTable(DefaultTableModel(12, 8))
    val pop = TableHeaderPopupMenu(table)
    val header = table.getTableHeader()
    header.setComponentPopupMenu(pop)
    pop.addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        cleanupHeader() // Java 9 doNotCloseOnMouseClick ArrayIndexOutOfBoundsException
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        cleanupHeader()
      }

      override fun popupMenuCanceled(e: PopupMenuEvent) {
        /* not needed */
      }

      private fun cleanupHeader() {
        header.setDraggedColumn(null)
        header.repaint()
      }
    })
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

class TableHeaderPopupMenu(table: JTable) : JPopupMenu() {
  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTableHeader)?.also {
      updateMenuItems(it.getColumnModel())
      super.show(c, x, y)
    }
  }

  private fun updateMenuItems(columnModel: TableColumnModel) {
    val isOnlyOneMenu = columnModel.getColumnCount() == 1
    if (isOnlyOneMenu) {
      children(this)
        .map { it.getComponent() }
        .forEach { it.setEnabled(it !is AbstractButton || !it.isSelected()) }
    } else {
      children(this)
        .forEach { it.getComponent().setEnabled(true) }
    }
  }

  private fun children(me: MenuElement): List<MenuElement> =
    me.getSubElements().map { children(it) }.fold(listOf(me)) { a, b -> a + b }

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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
