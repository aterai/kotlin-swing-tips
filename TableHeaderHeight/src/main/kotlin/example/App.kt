package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableColumn

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p = JPanel(GridLayout(2, 1))

    val table1 = makeTable()
    // Bad: >>>>
    val header = table1.getTableHeader()
    // Dimension d = header.getPreferredSize()
    // d.height = HEADER_HEIGHT
    // header.setPreferredSize(d) // addColumn case test
    header.setPreferredSize(Dimension(100, HEADER_HEIGHT))
    p.add(makeTitledPanel("Bad: JTableHeader#setPreferredSize(...)", JScrollPane(table1)))
    // <<<<

    val table2 = makeTable()
    val scroll = JScrollPane(table2)
    scroll.setColumnHeader(object : JViewport() {
      override fun getPreferredSize() = super.getPreferredSize().also {
        it.height = HEADER_HEIGHT
      }
    })
    p.add(makeTitledPanel("Override getPreferredSize()", scroll))

    val button = JButton("addColumn")
    button.addActionListener {
      listOf(table1, table2).forEach {
        it.getColumnModel().addColumn(TableColumn())
        val h = it.getTableHeader()
        val d = h.getPreferredSize()
        println(d)
      }
    }

    add(p)
    add(button, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTable() = JTable(DefaultTableModel(2, 20)).also {
    it.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(c)
  }

  companion object {
    const val HEADER_HEIGHT = 32
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
