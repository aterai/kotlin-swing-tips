package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.TabSet
import javax.swing.text.TabStop

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Double", "ALIGN_DECIMAL")
  val data = arrayOf(
    arrayOf("aaa", 1.4142, 1.4142),
    arrayOf("bbb", 98.765, 98.765),
    arrayOf("CCC", 1.73, 1.73),
    arrayOf("DDD", 0.0, 0.0),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      getColumnModel().getColumn(2).cellRenderer = AlignDecimalCellRenderer()
      autoCreateRowSorter = true
      rowSelectionAllowed = true
      fillsViewportHeight = true
      showVerticalLines = false
      showHorizontalLines = false
      isFocusable = false
      intercellSpacing = Dimension()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class AlignDecimalCellRenderer : TableCellRenderer {
  private val panel = JPanel(BorderLayout())
  private val textPane = object : JTextPane() {
    override fun getPreferredSize() = super.getPreferredSize().also {
      it.width = 60
    }

    override fun updateUI() {
      super.updateUI()
      isOpaque = false
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
      EventQueue.invokeLater {
        val attr = getStyle(StyleContext.DEFAULT_STYLE)
        val ts = arrayOf(TabStop(25f, TabStop.ALIGN_DECIMAL, TabStop.LEAD_NONE))
        StyleConstants.setTabSet(attr, TabSet(ts))
        setParagraphAttributes(attr, false)
      }
    }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    textPane.font = table.font
    textPane.text = "\t" + value?.toString()
    if (isSelected) {
      textPane.foreground = table.selectionForeground
      panel.background = table.selectionBackground
    } else {
      textPane.foreground = table.foreground
      panel.background = table.background
    }
    panel.add(textPane, BorderLayout.EAST)
    return panel
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
