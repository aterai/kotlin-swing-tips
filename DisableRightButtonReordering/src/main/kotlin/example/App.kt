package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.table.JTableHeader

class MainPanel : JPanel(BorderLayout()) {
  init {
    val c = JLayer(JScrollPane(makeJTable()), DisableRightButtonSwapLayerUI())
    val p = JPanel(GridLayout(2, 1))
    p.add(makeTitledPanel("Default", JScrollPane(makeJTable())))
    p.add(makeTitledPanel("Disable right mouse button reordering", c))
    add(p)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeJTable(): JTable {
    val table = JTable(4, 3)
    table.setAutoCreateRowSorter(true)
    val popup = object : JPopupMenu() {
      override fun show(c: Component, x: Int, y: Int) {
        (c as? JTableHeader)?.also { header ->
          header.setDraggedColumn(null)
          header.repaint()
          header.getTable().repaint()
          super.show(c, x, y)
        }
      }
    }
    popup.add("Item 1")
    popup.add("Item 2")
    popup.add("Item 3")
    table.getTableHeader().setComponentPopupMenu(popup)
    return table
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }
}

class DisableRightButtonSwapLayerUI : LayerUI<JScrollPane?>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_MOTION_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane?>) {
    val id = e.getID()
    val c = e.getComponent()
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
