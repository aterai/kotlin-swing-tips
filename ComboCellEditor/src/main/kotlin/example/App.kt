package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  UIManager.put("ComboBox.buttonDarkShadow", UIManager.getColor("TextField.foreground"))
  val comboModel = arrayOf("Name 0", "Name 1", "Name 2")
  val columnNames = arrayOf("Integer", "String", "Boolean")
  val data = arrayOf(
    arrayOf(12, comboModel[0], true),
    arrayOf(5, comboModel[2], false),
    arrayOf(92, comboModel[1], true),
    arrayOf(3, comboModel[0], false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  var col = table.columnModel.getColumn(0)
  col.minWidth = 60
  col.maxWidth = 60
  col.resizable = false
  col = table.columnModel.getColumn(1)
  col.cellEditor = DefaultCellEditor(makeComboBox(DefaultComboBoxModel(comboModel)))
  table.autoCreateRowSorter = true

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun <E> makeComboBox(model: ComboBoxModel<E>) = object : JComboBox<E>(model) {
  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder()
    val tmp = object : BasicComboBoxUI() {
      override fun createArrowButton() = super.createArrowButton().also {
        it.isContentAreaFilled = false
        it.border = BorderFactory.createEmptyBorder()
      }
    }
    setUI(tmp)
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
