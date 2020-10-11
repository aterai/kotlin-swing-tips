package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val comboModel = arrayOf("Name 0", "Name 1", "Name 2")
  val columnNames = arrayOf("Integer", "String", "Boolean")
  val data = arrayOf(
    arrayOf(12, comboModel[0], true),
    arrayOf(5, comboModel[2], false),
    arrayOf(92, comboModel[1], true),
    arrayOf(0, comboModel[0], false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    private val evenColor = Color(0xF0_F0_FA)
    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (isRowSelected(row)) {
        c.foreground = getSelectionForeground()
        c.background = getSelectionBackground()
      } else {
        c.foreground = foreground
        c.background = if (row % 2 == 0) evenColor else background
      }
      return c
    }
  }
  var col = table.columnModel.getColumn(0)
  col.minWidth = 60
  col.maxWidth = 60
  col.resizable = false
  UIManager.put("ComboBox.buttonDarkShadow", UIManager.getColor("TextField.foreground"))
  val combo = makeComboBox(DefaultComboBoxModel(comboModel))
  col = table.columnModel.getColumn(1)
  col.cellRenderer = ComboCellRenderer()
  col.cellEditor = DefaultCellEditor(combo)
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

private class ComboCellRenderer : TableCellRenderer {
  private var button: JButton? = null
  private val combo = object : JComboBox<String>() {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
      val tmp = object : BasicComboBoxUI() {
        override fun createArrowButton() = super.createArrowButton().also {
          button = it
          it.isContentAreaFilled = false
          it.border = BorderFactory.createEmptyBorder()
        }
      }
      setUI(tmp)
    }

    override fun isOpaque(): Boolean {
      val back = background
      val table = SwingUtilities.getAncestorOfClass(JTable::class.java, this)
      return if (table is JTable) {
        val colorMatch = back != null && back == table.background && table.isOpaque
        !colorMatch && super.isOpaque()
      } else {
        super.isOpaque()
      }
    }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    (combo.editor.editorComponent as? JTextField)?.also { editor ->
      editor.border = BorderFactory.createEmptyBorder()
      editor.isOpaque = true
      combo.removeAllItems()
      button?.also {
        if (isSelected) {
          editor.foreground = table.selectionForeground
          editor.background = table.selectionBackground
          it.background = table.selectionBackground
        } else {
          editor.foreground = table.foreground
          val bg = if (row % 2 == 0) EVEN_COLOR else table.background
          editor.background = bg
          it.background = bg
        }
      }
    }
    combo.addItem(value?.toString() ?: "")
    return combo
  }

  companion object {
    private val EVEN_COLOR = Color(0xF0_F0_FA)
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
