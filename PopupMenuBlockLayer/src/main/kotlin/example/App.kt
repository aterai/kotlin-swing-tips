package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }
  for (i in 0 until 100) {
    model.addRow(arrayOf("Name $i", i, false))
  }
  val table = object : JTable(model) {
    override fun getToolTipText(e: MouseEvent): String {
      val row = convertRowIndexToModel(rowAtPoint(e.point))
      val m = getModel()
      return "%s, %s".format(m.getValueAt(row, 0), m.getValueAt(row, 2))
    }
  }
  table.autoCreateRowSorter = true
  table.componentPopupMenu = TablePopupMenu()

  val layerUI = DisableInputLayerUI<Component>()
  val check = JCheckBox("Lock all(JScrollPane, JTable, JPopupMenu)")
  check.addActionListener {
    layerUI.setLocked((it.source as? JCheckBox)?.isSelected == true)
  }
  val scroll = JScrollPane(table)
  return JPanel(BorderLayout()).also {
    it.add(JLayer(scroll, layerUI))
    it.add(check, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DisableInputLayerUI<V : Component> : LayerUI<V>() {
  private val emptyMouseAdapter = object : MouseAdapter() { /* do nothing listener */ }
  private var isBlocking = false

  fun setLocked(flag: Boolean) {
    firePropertyChange(CMD_REPAINT, isBlocking, flag)
    isBlocking = flag
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.also {
      it.glassPane.addMouseListener(emptyMouseAdapter)
      it.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK or
        AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.KEY_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.also {
      it.layerEventMask = 0
      it.glassPane.removeMouseListener(emptyMouseAdapter)
    }
    super.uninstallUI(c)
  }

  override fun eventDispatched(
    e: AWTEvent,
    l: JLayer<out V>,
  ) {
    if (isBlocking && e is InputEvent) {
      e.consume()
    }
  }

  override fun applyPropertyChange(
    e: PropertyChangeEvent,
    l: JLayer<out V>,
  ) {
    if (CMD_REPAINT == e.propertyName) {
      l.glassPane.isVisible = e.newValue as? Boolean == true
    }
  }

  companion object {
    private const val CMD_REPAINT = "lock"
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        model.addRow(arrayOf("New row", model.rowCount, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
