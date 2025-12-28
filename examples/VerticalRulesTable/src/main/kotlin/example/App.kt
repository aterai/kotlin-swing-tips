package example

import java.awt.*
import java.awt.geom.Line2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val scroll = JScrollPane(makeTable())
  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(makeTable()))
    it.add(JLayer<JScrollPane>(scroll, BorderPaintLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("A1", "B1", "A2", "B2")
  val data = arrayOf<Array<Any>>(
    arrayOf(1, 33, 5, 7),
    arrayOf(2, 35, 6, 11),
    arrayOf(3, 34, 7, 12),
    arrayOf(4, 35, 8, 9),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private fun makeTable(): JTable {
  return object : JTable(makeModel()) {
    override fun updateUI() {
      super.updateUI()
      setFillsViewportHeight(true)
      setShowVerticalLines(false)
      setShowHorizontalLines(false)
      setIntercellSpacing(Dimension())
      setAutoCreateRowSorter(true)
      getTableHeader().setReorderingAllowed(false)
    }

    override fun prepareRenderer(
      renderer: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareRenderer(renderer, row, column)
      if (c is JComponent) {
        c.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3))
      }
      return c
    }
  }
}

private class BorderPaintLayerUI : LayerUI<JScrollPane>() {
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val view = c.getView()
      if (view is JScrollPane) {
        val viewport = view.getViewport()
        val table = viewport.view as? JTable ?: return
        paintVerticalRules(g, table, view)
      }
    }
  }

  private fun paintVerticalRules(g: Graphics, table: JTable, parent: JComponent) {
    val g2 = g.create() as Graphics2D
    g2.paint = UIManager.getColor("Table.gridColor")
    val columnCount = table.model.columnCount
    if (columnCount % 2 == 0) {
      val center = columnCount / 2 - 1
      val x1 = table.getCellRect(0, center, false).maxX
      val r = SwingUtilities.calculateInnerArea(parent, null)
      g2.draw(Line2D.Double(x1, r.getY(), x1, r.getHeight()))
      val x2 = x1 + 2.0
      g2.draw(Line2D.Double(x2, r.getY(), x2, r.getHeight()))
      g2.dispose()
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
