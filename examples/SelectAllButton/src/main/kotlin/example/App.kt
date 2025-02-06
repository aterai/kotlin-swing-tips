package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  val selectAllAction = object : AbstractAction("selectAll") {
    override fun actionPerformed(e: ActionEvent) {
      e.source = table
      table.actionMap["selectAll"].actionPerformed(e)
    }
  }
  val copyAction = object : AbstractAction("copy") {
    override fun actionPerformed(e: ActionEvent) {
      e.source = table
      table.actionMap["copy"].actionPerformed(e)
    }
  }

  val menuBar = JMenuBar()
  val menu = JMenu("Edit")
  menu.mnemonic = KeyEvent.VK_E
  menuBar.add(menu)
  val p = JPanel()
  listOf(selectAllAction, copyAction).forEach {
    menu.add(it)
    p.add(JButton(it))
  }

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(table)
  sp.bottomComponent = JScrollPane(JTextArea())
  sp.resizeWeight = .5

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
    EventQueue.invokeLater { p.rootPane.jMenuBar = menuBar }
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
