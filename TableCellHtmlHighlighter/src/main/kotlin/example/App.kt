package example

import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

private val WARNING_COLOR = Color(0xFF_C8_C8)

private val field = JTextField("ab+")
private val renderer = HighlightTableCellRenderer()
private val columnNames = arrayOf("A", "B")
private val data = arrayOf(
  arrayOf("aaa", "bb aa cc"),
  arrayOf("bbb", "def"),
  arrayOf("ccc bbb aaa bbb aae abe", "xxx"),
  arrayOf("ddd aaa bbb bbb", "cc bb aa"),
  arrayOf("cc cc bb bb aaa bb bb e", "xxx"),
  arrayOf("ddd aaa b bb bb", "cc bb aa"),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = String::class.java
}
private val sorter = TableRowSorter(model)

fun makeUI(): Component {
  val table = JTable(model)
  table.fillsViewportHeight = true
  table.rowSorter = sorter
  table.setDefaultRenderer(String::class.java, renderer)

  val dl = object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun removeUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun changedUpdate(e: DocumentEvent) {
      // not needed
    }
  }
  field.document.addDocumentListener(dl)
  fireDocumentChangeEvent()

  val sp = JPanel(BorderLayout(5, 5))
  sp.add(JLabel("regex pattern:"), BorderLayout.WEST)
  sp.add(field)
  sp.add(Box.createVerticalStrut(2), BorderLayout.SOUTH)
  sp.border = BorderFactory.createTitledBorder("Search")

  return JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(sp, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

fun fireDocumentChangeEvent() {
  field.background = Color.WHITE
  val pattern = field.text.trim()
  if (pattern.isEmpty()) {
    sorter.rowFilter = null
    renderer.updatePattern("")
  } else if (renderer.updatePattern(pattern)) {
    runCatching {
      sorter.setRowFilter(RowFilter.regexFilter(pattern))
    }.onFailure {
      field.background = WARNING_COLOR
    }
  }
}

private class HighlightTableCellRenderer : DefaultTableCellRenderer() {
  private var pattern = ""
  private var prev: String? = null

  fun updatePattern(str: String) = if (str == pattern) {
    false
  } else {
    prev = pattern
    pattern = str
    true
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    var txt = value?.toString() ?: ""
    if (pattern.isNotEmpty() && pattern != prev) {
      var pos = 0
      val buf = StringBuilder("<html>")
      pattern.toRegex().findAll(txt).map { it.range }.filterNot { it.isEmpty() }.forEach {
        val span = "%s<span style='color:#000000; background-color:#FFFF00'>%s</span>"
        val end = it.last + 1
        buf.append(span.format(txt.substring(pos, it.first), txt.substring(it.first, end)))
        pos = end
      }
      buf.append(txt.substring(pos))
      txt = buf.toString()
    }
    return super.getTableCellRendererComponent(
      table,
      txt,
      isSelected,
      hasFocus,
      row,
      column,
    )
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
