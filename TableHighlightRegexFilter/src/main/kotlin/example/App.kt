package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
// import javax.swing.text.Highlighter
// import javax.swing.text.Highlighter.HighlightPainter

class MainPanel : JPanel(BorderLayout(5, 5)) {
  private val field = JTextField("ab+")
  private val renderer = HighlightTableCellRenderer()

  private val columnNames = arrayOf("A", "B")
  private val data = arrayOf(
      arrayOf<Any>("aaa", "bbaacc"),
      arrayOf<Any>("bbb", "defg"),
      arrayOf<Any>("ccccbbbbaaabbbbaaeabee", "xxx"),
      arrayOf<Any>("dddaaabbbbb", "ccbbaa"),
      arrayOf<Any>("cc cc bbbb aaa bbbb e", "xxx"),
      arrayOf<Any>("ddd aaa b bbbb", "cc bbaa"))
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java
  }
  @Transient
  private val sorter = TableRowSorter<TableModel>(model)
  private val table = JTable(model)

  init {
    table.setFillsViewportHeight(true)
    table.setRowSorter(sorter)
    table.setDefaultRenderer(String::class.java, renderer)

    field.getDocument().addDocumentListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        fireDocumentChangeEvent()
      }

      override fun removeUpdate(e: DocumentEvent) {
        fireDocumentChangeEvent()
      }

      override fun changedUpdate(e: DocumentEvent) { /* not needed */ }
    })
    fireDocumentChangeEvent()

    val sp = JPanel(BorderLayout(5, 5))
    sp.add(JLabel("regex pattern:"), BorderLayout.WEST)
    sp.add(field)
    sp.add(Box.createVerticalStrut(2), BorderLayout.SOUTH)
    sp.setBorder(BorderFactory.createTitledBorder("Search"))

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(sp, BorderLayout.NORTH)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  protected fun fireDocumentChangeEvent() {
    field.setBackground(Color.WHITE)
    val pattern = field.getText().trim { it <= ' ' }
    if (pattern.isEmpty()) {
      sorter.setRowFilter(null)
      renderer.updatePattern("")
    } else if (renderer.updatePattern(pattern)) {
      runCatching {
        sorter.setRowFilter(RowFilter.regexFilter(pattern))
      }.onFailure {
        field.setBackground(WARNING_COLOR)
      }
    }
  }

  companion object {
    private val WARNING_COLOR = Color(0xFF_C8_C8)
  }
}

internal class HighlightTableCellRenderer : JTextField(), TableCellRenderer {
  @Transient
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
    setOpaque(true)
    setBorder(BorderFactory.createEmptyBorder())
    setForeground(Color.BLACK)
    setBackground(Color.WHITE)
    setEditable(false)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val txt = value?.toString() ?: ""
    val highlighter = getHighlighter()
    highlighter.removeAllHighlights()
    setText(txt)
    setBackground(if (isSelected) BACKGROUND_SELECTION_COLOR else Color.WHITE)
    if (!pattern.isEmpty() && pattern != prev) {
      // val matcher = Pattern.compile(pattern).matcher(txt)
      // var pos = 0
      // while (matcher.find(pos) && !matcher.group().isEmpty()) {
      //   val start = matcher.start()
      //   val end = matcher.end()
      //   try {
      //     highlighter.addHighlight(start, end, highlightPainter)
      //   } catch (ex: BadLocationException) {
      //     UIManager.getLookAndFeel().provideErrorFeedback(this)
      //   } catch (ex: PatternSyntaxException) {
      //     UIManager.getLookAndFeel().provideErrorFeedback(this)
      //   }
      //   pos = end
      // }
      runCatching {
        pattern.toRegex().findAll(txt).map { it.range }.filterNot { it.isEmpty() }.forEach {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
