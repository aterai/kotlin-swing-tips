package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

private const val ITEMS_PER_PAGE = 100
private var maxPageIndex = 0
private var currentPageIndex = 0

private val first = JButton("|<")
private val prev = JButton("<")
private val next = JButton(">")
private val last = JButton(">|")
private val columnNames = arrayOf("Year", "String", "Comment")
val model = object : DefaultTableModel(null, columnNames) {
  override fun getColumnClass(column: Int) =
    if (column == 0) Number::class.java else Any::class.java
}
private val sorter = TableRowSorter<TableModel>(model)
private val table = JTable(model)
private val field = JTextField(2)
private val label = JLabel("/ 1")

fun makeUI(): Component {
  currentPageIndex = 1
  table.fillsViewportHeight = true
  table.rowSorter = sorter
  table.isEnabled = false

  val po = JPanel().also {
    it.add(field)
    it.add(label)
  }

  val box = JPanel(GridLayout(1, 4, 2, 2))
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  listOf(first, prev, po, next, last).forEach { box.add(it) }

  val enterAction = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      currentPageIndex = field.text.toInt().coerceIn(1, maxPageIndex)
      initFilterAndButtons()
    }
  }

  val enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
  field.getInputMap(JComponent.WHEN_FOCUSED).put(enter, "Enter")
  field.actionMap.put("Enter", enterAction)
  (field.document as? AbstractDocument)?.documentFilter = IntegerDocumentFilter()
  listOf(first, prev, next, last).forEach {
    it.addActionListener { e -> updateCurrentPageIndex(e) }
  }
  TableUpdateTask(2021, ITEMS_PER_PAGE).execute()

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateCurrentPageIndex(e: ActionEvent) {
  currentPageIndex = when (e.source) {
    first -> 1
    prev -> currentPageIndex - 1
    next -> currentPageIndex + 1
    last -> maxPageIndex
    else -> currentPageIndex
  }
  initFilterAndButtons()
}

private class TableUpdateTask(max: Int, itemsPerPage: Int) : LoadTask(max, itemsPerPage) {
  init {
    field.isEditable = false
  }

  override fun process(chunks: List<List<Array<Any>>>) {
    if (isCancelled) {
      return
    }
    if (!table.isDisplayable) {
      println("process: DISPOSE_ON_CLOSE")
      cancel(true)
      return
    }
    chunks.forEach { it.forEach(model::addRow) }
    val rowCount = model.rowCount
    maxPageIndex = rowCount / ITEMS_PER_PAGE + if (rowCount % ITEMS_PER_PAGE == 0) 0 else 1
    initFilterAndButtons()
  }

  override fun done() {
    if (!table.isDisplayable) {
      println("done: DISPOSE_ON_CLOSE")
      cancel(true)
      return
    }
    val text = kotlin.runCatching {
      get()
    }.onFailure {
      if (it is InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }.getOrNull() ?: "Interrupted"
    println(text)
    table.isEnabled = true
    field.isEditable = true
  }
}

fun initFilterAndButtons() {
  sorter.rowFilter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
      val ti = currentPageIndex - 1
      val ei = entry.identifier
      return ti * ITEMS_PER_PAGE <= ei && ei < ti * ITEMS_PER_PAGE + ITEMS_PER_PAGE
    }
  }
  first.isEnabled = currentPageIndex > 1
  prev.isEnabled = currentPageIndex > 1
  next.isEnabled = currentPageIndex < maxPageIndex
  last.isEnabled = currentPageIndex < maxPageIndex
  field.text = currentPageIndex.toString()
  label.text = "/ $maxPageIndex"
}

private open class LoadTask(
  private val max: Int,
  private val itemsPerPage: Int
) : SwingWorker<String, List<Array<Any>>>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 1
    val c = max / itemsPerPage
    var i = 0
    while (i < c && !isCancelled) {
      current = load(current, itemsPerPage)
      i++
    }
    val surplus = max % itemsPerPage
    if (surplus > 0) {
      load(current, surplus)
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  protected fun load(current: Int, size: Int): Int {
    val result = (current until current + size).map {
      arrayOf<Any>(it, "Test: $it", if (it % 2 == 0) "" else "comment...")
    }.toList()
    Thread.sleep(500)
    publish(result)
    return current + result.size
  }
}

private class IntegerDocumentFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(fb: FilterBypass, offset: Int, text: String?, attr: AttributeSet?) {
    if (text != null) {
      replace(fb, offset, 0, text, attr)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(fb: FilterBypass, offset: Int, length: Int) {
    replace(fb, offset, length, "", null)
  }

  @Throws(BadLocationException::class)
  override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
    val doc = fb.document
    val currentLength = doc.length
    val currentContent = doc.getText(0, currentLength)
    val before = currentContent.substring(0, offset)
    val after = currentContent.substring(length + offset, currentLength)
    val newValue = before + (text ?: "") + after
    checkInput(newValue)
    fb.replace(offset, length, text, attrs)
  }

  companion object {
    @Throws(BadLocationException::class)
    private fun checkInput(proposedValue: String) {
      if (proposedValue.isNotEmpty()) {
        runCatching {
          proposedValue.toInt()
        }
      }
    }
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
