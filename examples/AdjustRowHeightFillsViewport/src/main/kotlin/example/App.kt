package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

private val columnNames = arrayOf("String", "Integer", "Boolean")
private val data = arrayOf<Array<Any>>(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val table = object : JTable(model) {
  private var prevHeight = -1
  private var prevRowCount = -1

  private fun adjustRowHeights(height: Int) {
    // Use the view row count, setRowHeight(int, int) takes a view index
    val rowCount = this.rowCount
    // setRowHeight(...) revalidates the table and calls doLayout() again,
    // so skip the update if neither the height nor the row count has changed
    if (rowCount > 0 && (height != prevHeight || rowCount != prevRowCount)) {
      val baseRowHeight = height / rowCount
      if (baseRowHeight > 0) {
        val remainder = height % rowCount
        // Distribute the remainder one pixel at a time to the first rows
        for (i in 0..<rowCount) {
          setRowHeight(i, baseRowHeight + if (i < remainder) 1 else 0)
        }
      }
    }
    prevHeight = height
    prevRowCount = rowCount
  }

  override fun getScrollableTracksViewportHeight() = getParent() is JViewport

  override fun doLayout() {
    super.doLayout()
    (getParent() as? JViewport)?.also {
      adjustRowHeights(it.extentSize.height)
    }
  }
}

fun createUI(): Component {
  val scroll = JScrollPane(table)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  // TEST: scroll.addComponentListener(new TableRowHeightAdjuster())
  scroll.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      (e.component as? JScrollPane)?.viewport?.view?.revalidate()
    }
  })

  val button = JButton("add")
  button.addActionListener { model.addRow(arrayOf<Any>("", 0, false)) }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

// // TEST when not considering adding rows
// class TableRowHeightAdjuster : ComponentAdapter() {
//   private var prevHeight = -1
//
//   override fun componentResized(e: ComponentEvent) {
//     val scroll = e.getComponent() as? JScrollPane ?: return
//     val table = scroll.getViewport().getView() as JTable
//     val height = scroll.getViewportBorderBounds().height
//     val rowCount = table.getModel().getRowCount()
//     val rowHeight = height / rowCount
//     if (height != prevHeight && rowHeight > 0) {
//       var remainder = height % rowCount
//       for (i in 0..<rowCount) {
//         val a = if (remainder > 0) if (i == rowCount - 1) remainder else 1 else 0
//         table.setRowHeight(i, rowHeight + a)
//         remainder--
//       }
//       prevHeight = height
//     }
//   }
// }

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
