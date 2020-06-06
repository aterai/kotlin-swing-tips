package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI() = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
  it.topComponent = JScrollPane(makeJTable())
  it.bottomComponent = JLayer(JScrollPane(makeJTable()), TableHeaderFillerLayerUI())
  it.resizeWeight = .5
  it.preferredSize = Dimension(320, 240)
}

private fun makeJTable() = JTable(4, 3).also {
  it.autoResizeMode = JTable.AUTO_RESIZE_OFF
  it.autoCreateRowSorter = true
}

private class TableHeaderFillerLayerUI : LayerUI<JScrollPane>() {
  private val tempTable = JTable(DefaultTableModel(arrayOf(""), 0))
  private val filler = tempTable.tableHeader
  private val fillerColumn = tempTable.columnModel.getColumn(0)

  override fun paint(g: Graphics?, c: JComponent) {
    super.paint(g, c)
    val scroll = (c as? JLayer<*>)?.view as? JScrollPane ?: return
    val table = scroll.viewport.view as? JTable ?: return
    val header = table.tableHeader

    var width = header.width
    val cm = header.columnModel
    for (i in 0 until cm.columnCount) {
      width -= cm.getColumn(i).width
    }

    val pt = SwingUtilities.convertPoint(header, 0, 0, c)
    filler.setLocation(pt.x + header.width - width, pt.y)
    filler.setSize(width, header.height)
    fillerColumn.width = width

    SwingUtilities.paintComponent(g, filler, tempTable, filler.bounds)
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.COMPONENT_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent?) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processComponentEvent(e: ComponentEvent, l: JLayer<out JScrollPane>) {
    val c = e.component as? JTableHeader ?: return
    l.repaint(c.bounds)
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
