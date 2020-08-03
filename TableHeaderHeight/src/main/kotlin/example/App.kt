package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableColumn

const val HEADER_HEIGHT = 32

fun makeUI(): Component {
  val p = JPanel(GridLayout(2, 1))

  val table1 = makeTable()
  // Bad: >>>>
  val header = table1.tableHeader
  // Dimension d = header.getPreferredSize()
  // d.height = HEADER_HEIGHT
  // header.preferredSize = d // addColumn case test
  header.preferredSize = Dimension(100, HEADER_HEIGHT)
  p.add(makeTitledPanel("Bad: JTableHeader#setPreferredSize(...)", JScrollPane(table1)))
  // <<<<

  val table2 = makeTable()
  val scroll = JScrollPane(table2)
  scroll.columnHeader = object : JViewport() {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.height = HEADER_HEIGHT
    }
  }
  p.add(makeTitledPanel("Override getPreferredSize()", scroll))

  val button = JButton("addColumn")
  button.addActionListener {
    listOf(table1, table2).forEach {
      it.columnModel.addColumn(TableColumn())
      val h = it.tableHeader
      val d = h.preferredSize
      println(d)
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(button, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable() = JTable(DefaultTableModel(2, 20)).also {
  it.autoResizeMode = JTable.AUTO_RESIZE_OFF
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
