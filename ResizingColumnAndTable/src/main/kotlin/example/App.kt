package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val table: JTable = object : JTable(100, 3) {
    // https://stackoverflow.com/questions/16368343/jtable-resize-only-selected-column-when-container-size-changes
    // https://stackoverflow.com/questions/23201818/jtable-columns-doesnt-resize-probably-when-jframe-resize
    override fun doLayout() {
      getTableHeader()?.also { header ->
        if (header.resizingColumn == null && getAutoResizeMode() == AUTO_RESIZE_LAST_COLUMN) {
          val tcm = getColumnModel()
          header.resizingColumn = tcm.getColumn(tcm.columnCount - 1)
        }
      }
      super.doLayout()
    }
  }
  // table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN)
  return JPanel(GridLayout(0, 1)).also {
    it.add(makeTitledPanel("Normal JTable.AUTO_RESIZE_LAST_COLUMN", JTable(100, 3)))
    it.add(makeTitledPanel("Resize only last column when JTable resized", table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, table: JTable): Component {
  table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(JScrollPane(table))
  return p
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
