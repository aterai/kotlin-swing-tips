package example

import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter

private val WARNING_COLOR = Color(0xFF_C8_C8)

private val field = JTextField("ab+")
private val renderer = HighlightTableCellRenderer()
private val columnNames = arrayOf("A", "B")
private val data = arrayOf(
  arrayOf("aaa", "111111"),
  arrayOf("bbb", "22222"),
  arrayOf("333333333333333", "xxx"),
  arrayOf("4444444444444", "55555555"),
  arrayOf("cc cc bbb1 aaa bbb2 e", "xxx"),
  arrayOf("ddd aaa b bbb3", "cc #aabbcc"),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = String::class.java
}

private val sorter = TableRowSorter<TableModel>(model)
private val table = JTable(model)

fun makeUI(): Component {
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

private fun fireDocumentChangeEvent() {
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

private class HighlightTableCellRenderer :
  JTextField(),
  TableCellRenderer {
  private val highlightPainter = DefaultHighlightPainter(Color.YELLOW)
  private var pattern = ""
  private var prev: String? = null

  fun updatePattern(str: String) = if (str == pattern) {
    false
  } else {
    prev = pattern
    pattern = str
    true
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = true
    border = BorderFactory.createEmptyBorder()
    foreground = Color.BLACK
    background = Color.WHITE
    isEditable = false
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val txt = value?.toString() ?: ""
    val highlighter = highlighter
    highlighter.removeAllHighlights()
    text = txt
    background = if (isSelected) BACKGROUND_SELECTION_COLOR else Color.WHITE
    if (pattern.isNotEmpty() && pattern != prev) {
      runCatching {
        pattern
          .toRegex()
          .findAll(
            txt,
          ).map { it.range }
          .filterNot { it.isEmpty() }
          .forEach {
            highlighter.addHighlight(it.first(), it.last() + 1, highlightPainter)
          }
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(this)
      }
    }
    return this
  }

  companion object {
    private val BACKGROUND_SELECTION_COLOR = Color(220, 240, 255)
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
