package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val see = "See Also: Constant Field Values"
    val columnNames = arrayOf("AAA", "BBB")
    val data = arrayOf(
      arrayOf(makeOptionPaneDescription("error"), see),
      arrayOf(makeOptionPaneDescription("information"), see),
      arrayOf(makeOptionPaneDescription("question"), see),
      arrayOf(makeOptionPaneDescription("warning"), see)
    )
    val model = object : DefaultTableModel(data, columnNames) {
      override fun isCellEditable(row: Int, column: Int) = false
    }
    val table = JTable(model)
    table.setAutoCreateRowSorter(true)
    table.getTableHeader().setReorderingAllowed(false)
    table.setRowSelectionAllowed(true)
    table.setFillsViewportHeight(true)
    table.setShowVerticalLines(false)
    table.setIntercellSpacing(Dimension(0, 1))
    table.setRowHeight(56)
    val renderer = ColumnSpanningCellRenderer()
    for (i in 0 until table.getColumnModel().getColumnCount()) {
      table.getColumnModel().getColumn(i).also {
        it.setCellRenderer(renderer)
        it.setMinWidth(50)
      }
    }
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeOptionPaneDescription(type: String): OptionPaneDescription {
    val key = type + "Icon"
    val icon = UIManager.getIcon("OptionPane.$key")
    val fmt = "public static final int %s_MESSAGE%nUsed for %s messages."
    val msg = fmt.format(type.toUpperCase(Locale.ENGLISH), type)
    return OptionPaneDescription(key, icon, msg)
  }
}

class ColumnSpanningCellRenderer : JPanel(BorderLayout()), TableCellRenderer {
  private val textArea = JTextArea(2, 999_999)
  private val label = JLabel()
  private val iconLabel = JLabel()
  private val scroll = JScrollPane(textArea)
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val d: OptionPaneDescription
    if (value is OptionPaneDescription) {
      d = value
      add(iconLabel, BorderLayout.WEST)
    } else {
      val title = value?.toString() ?: ""
      val mri = table.convertRowIndexToModel(row)
      val o = table.getModel().getValueAt(mri, 0)
      d = if (o is OptionPaneDescription) {
        OptionPaneDescription(title, o.icon, o.text)
      } else {
        OptionPaneDescription(title, null, "")
      }
      remove(iconLabel)
    }
    label.setText(d.title)
    textArea.setText(d.text)
    iconLabel.setIcon(d.icon)
    val cr = table.getCellRect(row, column, false)
    if (column != TARGET_IDX) {
      cr.x -= iconLabel.getPreferredSize().width
    }
    scroll.getViewport().setViewPosition(cr.getLocation())
    setBackground(if (isSelected) Color.ORANGE else Color.WHITE)
    return this
  }

  companion object {
    private const val TARGET_IDX = 0
  }

  init {
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scroll.setBorder(BorderFactory.createEmptyBorder())
    scroll.setViewportBorder(BorderFactory.createEmptyBorder())
    scroll.setOpaque(false)
    scroll.getViewport().setOpaque(false)
    textArea.setBorder(BorderFactory.createEmptyBorder())
    textArea.setMargin(Insets(0, 0, 0, 0))
    textArea.setForeground(Color.RED)
    textArea.setEditable(false)
    textArea.setFocusable(false)
    textArea.setOpaque(false)
    iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4))
    iconLabel.setOpaque(false)
    val b1 = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    val b2 = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY)
    label.setBorder(BorderFactory.createCompoundBorder(b2, b1))
    setBackground(textArea.background)
    setOpaque(true)
    add(label, BorderLayout.NORTH)
    add(scroll)
  }
}

data class OptionPaneDescription(val title: String, val icon: Icon?, val text: String)

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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
