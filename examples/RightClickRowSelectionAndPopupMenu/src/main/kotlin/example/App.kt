package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.LayerUI

fun createUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("Default", JScrollPane(createTable(createPopupMenu())))
  tabbedPane.addTab("MouseListener", JScrollPane(createMouseListenerTable()))
  tabbedPane.addTab("PopupMenuListener", JScrollPane(createPopupMenuListenerTable()))
  val scroll = JScrollPane(createTable(createPopupMenu()))
  tabbedPane.addTab("JLayer", JLayer(scroll, RightMouseButtonLayerUI()))
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTable(popup: JPopupMenu): JTable {
  val table = JTable(10, 5)
  table.setFillsViewportHeight(true)
  table.setComponentPopupMenu(popup)
  return table
}

private fun createMouseListenerTable(): JTable {
  val table = createTable(createPopupMenu())
  table.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val c = e.component
      if (c is JTable && SwingUtilities.isRightMouseButton(e)) {
        updateSelection(c, e.point)
      }
    }
  })
  return table
}

private fun createPopupMenuListenerTable(): JTable {
  val popup = createPopupMenu()
  val table = createTable(popup)
  popup.addPopupMenuListener(object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      // The location of the JPopupMenu is not yet determined at this point
      SwingUtilities.invokeLater {
        val pt = SwingUtilities.convertPoint(popup, Point(), table)
        updateSelection(table, pt)
      }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }
  })
  return table
}

private fun createPopupMenu(): JPopupMenu {
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

private class RightMouseButtonLayerUI : LayerUI<JScrollPane>() {
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
    l: JLayer<out JScrollPane>,
  ) {
    val c = e.component
    // Update the selection only once on MOUSE_PRESSED, not on MOUSE_RELEASED or MOUSE_CLICKED
    val pressed = e.id == MouseEvent.MOUSE_PRESSED
    if (pressed && c is JTable && SwingUtilities.isRightMouseButton(e)) {
      updateSelection(c, e.point)
    }
  }
}

fun updateSelection(
  table: JTable,
  pt: Point,
) {
  if (table.isEditing) {
    table.removeEditor()
  }
  val row = table.rowAtPoint(pt)
  val column = table.columnAtPoint(pt)
  // rowAtPoint(...) or columnAtPoint(...) returns -1 outside the cell area
  if (row < 0 || column < 0) {
    table.clearSelection()
  } else if (!table.isRowSelected(row)) {
    table.changeSelection(row, column, false, false)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
