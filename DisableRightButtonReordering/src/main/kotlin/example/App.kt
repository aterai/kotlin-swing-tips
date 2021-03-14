package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.table.JTableHeader

fun makeUI() = JPanel(BorderLayout()).also {
  val c = JLayer(JScrollPane(makeJTable()), DisableRightButtonSwapLayerUI())
  val p = JPanel(GridLayout(2, 1))
  p.add(makeTitledPanel("Default", JScrollPane(makeJTable())))
  p.add(makeTitledPanel("Disable right mouse button reordering", c))
  it.add(p)
  it.preferredSize = Dimension(320, 240)
}

private fun makeJTable(): JTable {
  val table = JTable(4, 3)
  table.autoCreateRowSorter = true
  val popup = object : JPopupMenu() {
    override fun show(c: Component, x: Int, y: Int) {
      (c as? JTableHeader)?.also { header ->
        header.draggedColumn = null
        header.repaint()
        header.table.repaint()
        super.show(c, x, y)
      }
    }
  }
  popup.add("Item 1")
  popup.add("Item 2")
  popup.add("Item 3")
  table.tableHeader.componentPopupMenu = popup
  return table
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class DisableRightButtonSwapLayerUI : LayerUI<JScrollPane>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val id = e.id
    val c = e.component
    if (c is JTableHeader && id == MouseEvent.MOUSE_DRAGGED && SwingUtilities.isRightMouseButton(e)) {
      e.consume()
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
