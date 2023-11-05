package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val table = JTable(10, 5)
  table.setFillsViewportHeight(true)
  table.setComponentPopupMenu(makePopupMenu())
  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("Default", JScrollPane(table))
  tabbedPane.addTab("MouseListener", JScrollPane(makeTable1()))
  tabbedPane.addTab("PopupMenuListener", JScrollPane(makeTable2()))
  val table3 = JTable(10, 5)
  table3.setFillsViewportHeight(true)
  table3.setComponentPopupMenu(makePopupMenu())
  val scroll = JScrollPane(table3)
  tabbedPane.addTab("JLayer", JLayer(scroll, RightMouseButtonLayerUI()))

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable1(): JTable {
  val table = JTable(10, 5)
  table.setFillsViewportHeight(true)
  table.setComponentPopupMenu(makePopupMenu())
  table.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val t = e.component
      if (t is JTable && SwingUtilities.isRightMouseButton(e)) {
        if (t.isEditing) {
          t.removeEditor()
        }
        val pt = e.getPoint()
        val r = getCellArea(t)
        if (r.contains(pt)) {
          val currentRow = t.rowAtPoint(pt)
          val currentColumn = t.columnAtPoint(pt)
          if (isNotRowContains(t.getSelectedRows(), currentRow)) {
            t.changeSelection(currentRow, currentColumn, false, false)
          }
        } else {
          t.clearSelection()
        }
      }
    }
  })
  return table
}

private fun makeTable2(): JTable {
  val table = JTable(10, 5)
  table.setFillsViewportHeight(true)
  val popup = makePopupMenu()
  popup.addPopupMenuListener(object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      if (table.isEditing) {
        table.removeEditor()
      }
      SwingUtilities.invokeLater {
        val pt = SwingUtilities.convertPoint(popup, Point(), table)
        val r = getCellArea(table)
        if (r.contains(pt)) {
          val currentRow = table.rowAtPoint(pt)
          val currentColumn = table.columnAtPoint(pt)
          if (isNotRowContains(table.getSelectedRows(), currentRow)) {
            table.changeSelection(currentRow, currentColumn, false, false)
          }
        } else {
          table.clearSelection()
        }
      }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }
  })
  table.setComponentPopupMenu(popup)
  return table
}

private fun makePopupMenu(): JPopupMenu {
  val popup = JPopupMenu()
  popup.add("clearSelection").addActionListener {
    val c = popup.invoker
    if (c is JTable) {
      c.clearSelection()
    }
  }
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  return popup
}

private class RightMouseButtonLayerUI : LayerUI<JScrollPane?>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out JScrollPane?>,
  ) {
    val table = e.component
    if (table is JTable && SwingUtilities.isRightMouseButton(e)) {
      if (table.isEditing) {
        table.removeEditor()
      }
      val pt = e.getPoint()
      val r = getCellArea(table)
      if (r.contains(pt)) {
        val currentRow = table.rowAtPoint(pt)
        val currentColumn = table.columnAtPoint(pt)
        if (isNotRowContains(table.getSelectedRows(), currentRow)) {
          table.changeSelection(currentRow, currentColumn, false, false)
        }
      } else {
        table.clearSelection()
      }
    } else {
      super.processMouseEvent(e, l)
    }
  }
}

fun getCellArea(table: JTable): Rectangle {
  val start = table.getCellRect(0, 0, true)
  val rc = table.getRowCount()
  val cc = table.columnCount
  val end = table.getCellRect(rc - 1, cc - 1, true)
  return start.union(end)
}

fun isNotRowContains(
  selectedRows: IntArray,
  currentRow: Int,
): Boolean {
  for (i in selectedRows) {
    if (i == currentRow) {
      return false
    }
  }
  return true
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
