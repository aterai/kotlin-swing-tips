package example

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn

class HeaderCheckBoxHandler(val table: JTable, val targetColumnIndex: Int) : MouseAdapter(), TableModelListener {

  override fun tableChanged(e: TableModelEvent) {
    if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == targetColumnIndex) {
      val vci = table.convertColumnIndexToView(targetColumnIndex)
      val column = table.getColumnModel().getColumn(vci)
      val status = column.getHeaderValue()
      val m = table.getModel()
      if (m is DefaultTableModel && fireUpdateEvent(m, column, status)) {
        val h = table.getTableHeader()
        h.repaint(h.getHeaderRect(vci))
      }
    }
  }

  private fun fireUpdateEvent(m: DefaultTableModel, column: TableColumn, status: Any): Boolean {
    return if (Status.INDETERMINATE == status) {
      // val l = (m.getDataVector() as Vector<*>).stream()
      //     .map { v -> (v as Vector<*>).get(targetColumnIndex) as Boolean }
      //     .distinct()
      //     .collect(Collectors.toList())
      val l = m.getDataVector().map { (it as List<*>).get(targetColumnIndex) as Boolean }.distinct()
      val isOnlyOneSelected = l.size == 1
      if (isOnlyOneSelected) {
        // column.setHeaderValue(if (l.get(0)) Status.SELECTED else Status.DESELECTED)
        column.setHeaderValue(if (l.first()) Status.SELECTED else Status.DESELECTED)
        true
      } else {
        false
      }
    } else {
      column.setHeaderValue(Status.INDETERMINATE)
      true
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.getComponent() as JTableHeader
    val tbl = header.getTable()
    val columnModel = tbl.getColumnModel()
    val m = tbl.getModel()
    val vci = columnModel.getColumnIndexAtX(e.getX())
    val mci = tbl.convertColumnIndexToModel(vci)
    if (mci == targetColumnIndex && m.getRowCount() > 0) {
      val column = columnModel.getColumn(vci)
      val b = Status.DESELECTED === column.getHeaderValue()
      for (i in 0 until m.getRowCount()) {
        m.setValueAt(b, i, mci)
      }
      column.setHeaderValue(if (b) Status.SELECTED else Status.DESELECTED)
      // header.repaint();
    }
  }
}
