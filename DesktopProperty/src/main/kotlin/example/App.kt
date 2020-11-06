package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

private val columnNames = arrayOf("Name", "Class", "Value")
private val model = DefaultTableModel(null, columnNames)

private fun initModel(e: PropertyChangeEvent?) {
  if (e != null) {
    println("----\n${e.propertyName}\n")
    println(Toolkit.getDefaultToolkit().getDesktopProperty(e.propertyName))
  }
  model.rowCount = 0
  val tk = Toolkit.getDefaultToolkit()
  (tk.getDesktopProperty("win.propNames") as? Array<*>)?.forEach {
    val o = tk.getDesktopProperty(it.toString())
    val row = arrayOf(it, o.javaClass, o)
    model.addRow(row)
  }
}

fun makeUI(): Component {
  val table = object : JTable(model) {
    override fun isCellEditable(row: Int, column: Int) = false
  }
  table.autoCreateRowSorter = true
  val l = PropertyChangeListener { e -> initModel(e) }
  Toolkit.getDefaultToolkit().addPropertyChangeListener("win.xpstyle.colorName", l)
  Toolkit.getDefaultToolkit().addPropertyChangeListener("awt.multiClickInterval", l)
  initModel(null)
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
