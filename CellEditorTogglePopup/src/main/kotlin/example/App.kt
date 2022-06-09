package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val model = arrayOf("Item 0", "Item 1", "Item 2")
  val columnNames = arrayOf("Default", "setEnabled", "String")
  val data = arrayOf(
    arrayOf(model[0], model[0], "aaa"),
    arrayOf(model[1], model[2], "bbb")
  )
  val table = JTable(DefaultTableModel(data, columnNames))
  table.rowHeight = 20
  table.columnModel.getColumn(0).cellEditor = DefaultCellEditor(JComboBox(model))
  val comboBox = JComboBox(model)
  comboBox.addAncestorListener(object : AncestorListener {
    override fun ancestorAdded(e: AncestorEvent) {
      // println("ancestorAdded")
      val c = e.component
      c.isEnabled = false
      EventQueue.invokeLater {
        // println("invokeLater")
        c.isEnabled = true
      }
    }

    override fun ancestorRemoved(e: AncestorEvent) {
      /* not needed */
    }

    override fun ancestorMoved(e: AncestorEvent) {
      /* not needed */
    }
  })
  table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(comboBox)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
