package example

import java.awt.*
import javax.swing.*
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

  val info = JTextField()
  info.isEditable = false

  val button = JButton("addColumn")
  button.addActionListener {
    table1.columnModel.addColumn(TableColumn())
    table2.columnModel.addColumn(TableColumn())
    info.text = "%s - %s".format(getDim(table1), getDim(table2))
  }

  val box = Box.createHorizontalBox()
  box.add(button)
  box.add(info)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getDim(t: JTable): String {
  val d = t.tableHeader.preferredSize
  return "%dx%d".format(d.width, d.height)
}

private fun makeTable() = JTable(DefaultTableModel(2, 20)).also {
  it.autoResizeMode = JTable.AUTO_RESIZE_OFF
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
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
