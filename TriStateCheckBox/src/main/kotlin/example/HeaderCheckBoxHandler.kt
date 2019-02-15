// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Vector
import java.util.stream.Collectors
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
    if (Status.INDETERMINATE.equals(status)) {
      val l = (m.getDataVector() as Vector<*>)
          .stream()
          .map { v -> (v as Vector<*>).get(targetColumnIndex) as Boolean }
          .distinct()
          .collect(Collectors.toList())
      val isOnlyOneSelected = l.size == 1
      if (isOnlyOneSelected) {
        column.setHeaderValue(if (l.get(0)) Status.SELECTED else Status.DESELECTED)
        return true
      } else {
        return false
      }
    } else {
      column.setHeaderValue(Status.INDETERMINATE)
      return true
    }
  }
  // private boolean fireUpdateEvent(TableModel m, TableColumn column, Object status) {
  //   if (Status.INDETERMINATE.equals(status)) {
  //     boolean selected = true;
  //     boolean deselected = true;
  //     for (int i = 0; i < m.getRowCount(); i++) {
  //       Boolean b = (Boolean) m.getValueAt(i, targetColumnIndex);
  //       selected &= b;
  //       deselected &= !b;
  //       if (selected == deselected) {
  //         return false;
  //       }
  //     }
  //     if (deselected) {
  //       column.setHeaderValue(Status.DESELECTED);
  //     } else if (selected) {
  //       column.setHeaderValue(Status.SELECTED);
  //     } else {
  //       return false;
  //     }
  //   } else {
  //     column.setHeaderValue(Status.INDETERMINATE);
  //   }
  //   return true;
  // }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.getComponent() as JTableHeader
    val tbl = header.getTable()
    val columnModel = tbl.getColumnModel()
    val m = tbl.getModel()
    val vci = columnModel.getColumnIndexAtX(e.getX())
    val mci = tbl.convertColumnIndexToModel(vci)
    if (mci == targetColumnIndex && m.getRowCount() > 0) {
      val column = columnModel.getColumn(vci)
      val v = column.getHeaderValue()
      val b = Status.DESELECTED.equals(v)
      for (i in 0 until m.getRowCount()) {
        m.setValueAt(b, i, mci)
      }
      column.setHeaderValue(if (b) Status.SELECTED else Status.DESELECTED)
      // header.repaint();
    }
  }
}
