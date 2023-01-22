package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.invoke.MethodHandles
import java.util.logging.Logger
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)

fun makeUI(): Component {
  val columnNames = arrayOf("Name", "Class", "Value")
  val model = DefaultTableModel(null, columnNames)
  val table = object : JTable(model) {
    override fun isCellEditable(row: Int, column: Int) = false
  }
  table.autoCreateRowSorter = true
  val l = PropertyChangeListener { e -> updateModel(model, e) }
  Toolkit.getDefaultToolkit().addPropertyChangeListener("win.xpstyle.colorName", l)
  Toolkit.getDefaultToolkit().addPropertyChangeListener("awt.multiClickInterval", l)
  updateModel(model, null)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateModel(model: DefaultTableModel, e: PropertyChangeEvent?) {
  if (e != null) {
    logger.info {
      val n = e.propertyName
      val p = Toolkit.getDefaultToolkit().getDesktopProperty(n)
      "$n: $p\n"
    }
  }
  model.rowCount = 0
  val tk = Toolkit.getDefaultToolkit()
  (tk.getDesktopProperty("win.propNames") as? Array<*>)?.forEach {
    val o = tk.getDesktopProperty(it.toString())
    val row = arrayOf(it, o.javaClass, o)
    model.addRow(row)
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
