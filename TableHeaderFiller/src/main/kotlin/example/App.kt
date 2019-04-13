package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class MainPanel : JPanel(BorderLayout()) {
  init {
    add(JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
      it.setTopComponent(JScrollPane(makeJTable()))
      it.setBottomComponent(JLayer<JScrollPane>(JScrollPane(makeJTable()), TableHeaderFillerLayerUI()))
      it.setResizeWeight(.5)
    })
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeJTable() = JTable(4, 3).also {
    it.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    it.setAutoCreateRowSorter(true)
  }
}

internal class TableHeaderFillerLayerUI : LayerUI<JScrollPane>() {
  private val tempTable = JTable(DefaultTableModel(arrayOf<Any>(""), 0))
  private val filler = tempTable.getTableHeader()
  private val fillerColumn = tempTable.getColumnModel().getColumn(0)

  override fun paint(g: Graphics?, c: JComponent) {
    super.paint(g, c)
    val layer = c as? JLayer<*> ?: return
    val scroll = layer.getView() as JScrollPane
    val table = scroll.getViewport().getView() as JTable
    val header = table.getTableHeader()

    var width = header.getWidth()
    val cm = header.getColumnModel()
    for (i in 0 until cm.getColumnCount()) {
      width -= cm.getColumn(i).getWidth()
    }

    val pt = SwingUtilities.convertPoint(header, 0, 0, c)
    filler.setLocation(pt.x + header.getWidth() - width, pt.y)
    filler.setSize(width, header.getHeight())
    fillerColumn.setWidth(width)

    SwingUtilities.paintComponent(g, filler, tempTable, filler.getBounds())
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.COMPONENT_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent?) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  protected override fun processComponentEvent(e: ComponentEvent, l: JLayer<out JScrollPane>) {
    val c = e.getComponent() as? JTableHeader ?: return
    l.repaint(c.getBounds())
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
