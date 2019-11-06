package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.TabSet
import javax.swing.text.TabStop

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf("String", "Double", "ALIGN_DECIMAL")
  private val data = arrayOf(
    arrayOf("aaa", 1.4142, 1.4142),
    arrayOf("bbb", 98.765, 98.765),
    arrayOf("CCC", 1.73, 1.73),
    arrayOf("DDD", 0.0, 0.0)
  )
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }

  init {
    val table: JTable = object : JTable(model) {
      override fun updateUI() {
        super.updateUI()
        getColumnModel().getColumn(2).setCellRenderer(AlignDecimalCellRenderer())
      }
    }
    table.setAutoCreateRowSorter(true)
    table.setRowSelectionAllowed(true)
    table.setFillsViewportHeight(true)
    table.setShowVerticalLines(false)
    table.setShowHorizontalLines(false)
    table.setFocusable(false)
    table.setIntercellSpacing(Dimension())
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

class AlignDecimalCellRenderer : TableCellRenderer {
  private val panel = JPanel(BorderLayout())
  private val textPane: JTextPane = object : JTextPane() {
    override fun getPreferredSize() = super.getPreferredSize().also {
      it.width = 60
    }

    override fun updateUI() {
      super.updateUI()
      setOpaque(false)
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, java.lang.Boolean.TRUE)
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
    column: Int
  ): Component {
    textPane.setFont(table.getFont())
    textPane.setText("\t" + value?.toString())
    if (isSelected) {
      textPane.setForeground(table.getSelectionForeground())
      panel.setBackground(table.getSelectionBackground())
    } else {
      textPane.setForeground(table.getForeground())
      panel.setBackground(table.getBackground())
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
