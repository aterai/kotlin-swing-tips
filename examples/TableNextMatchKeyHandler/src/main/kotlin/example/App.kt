package example

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.Locale
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.text.Position.Bias

fun createUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("aaa", 15, true),
    arrayOf("bbb", 6, false),
    arrayOf("abc", 92, true),
    arrayOf("Bbb", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.putClientProperty("JTable.autoStartsEdit", false)
  table.autoCreateRowSorter = true
  table.addKeyListener(TableNextMatchKeyHandler())
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

// @see javax/swing/plaf/basic/BasicListUI.Handler
// @see javax/swing/plaf/basic/BasicTreeUI.Handler
private class TableNextMatchKeyHandler : KeyAdapter() {
  private var prefix = ""
  private var typedString = ""
  private var lastTime = 0L

  private fun isNavigationKey(e: KeyEvent): Boolean {
    val table = e.component as? JTable ?: return false
    val inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    val key = KeyStroke.getKeyStrokeForEvent(e)
    return inputMap != null && inputMap[key] != null
  }

  override fun keyPressed(e: KeyEvent) {
    if (isNavigationKey(e)) {
      prefix = ""
      typedString = ""
      lastTime = 0L
    }
  }

  override fun keyTyped(e: KeyEvent) {
    val table = e.component as? JTable ?: return
    if (table.rowCount > 0 && isPrefixKey(e)) {
      val backward = e.isShiftDown
      val startRow = updatePrefix(e.keyChar, e.getWhen(), table.selectedRow, backward)
      selectNextMatch(table, startRow, backward)
    }
  }

  private fun isPrefixKey(e: KeyEvent): Boolean {
    val modified = e.isAltDown || e.isControlDown || e.isMetaDown
    // Skip control characters such as Enter, Tab, Backspace or Escape
    return !modified && !Character.isISOControl(e.keyChar) && !isNavigationKey(e)
  }

  private fun updatePrefix(
    c: Char,
    time: Long,
    selectedRow: Int,
    backward: Boolean,
  ): Int {
    var startRow = selectedRow
    if (time - lastTime < TIME_FACTOR) {
      typedString += c
      if (prefix.length == 1 && c == prefix[0]) {
        // Subsequent same key presses move the keyboard focus to the next
        // object that starts with the same letter.
        startRow += if (backward) -1 else 1
      } else {
        prefix = typedString
      }
    } else {
      startRow += if (backward) -1 else 1
      typedString = c.toString()
      prefix = typedString
    }
    lastTime = time
    return startRow
  }

  private fun selectNextMatch(
    table: JTable,
    startRow: Int,
    backward: Boolean,
  ) {
    val rowCount = table.rowCount
    val start = if (startRow in 0..<rowCount) {
      startRow
    } else if (backward) {
      rowCount - 1
    } else {
      0
    }
    val bias = if (backward) Bias.Backward else Bias.Forward
    // getNextMatch(...) searches all rows cyclically, so no extra wrap search is needed
    val row = getNextMatch(table, prefix, start, bias)
    if (row >= 0) {
      val column = maxOf(0, table.convertColumnIndexToView(TARGET_COLUMN))
      // JTable#changeSelection(...) also scrolls the cell to visible if autoscrolls is true
      table.changeSelection(row, column, false, false)
    }
  }

  companion object {
    // model column index of the search target, independent of column reordering
    private const val TARGET_COLUMN = 0
    private const val TIME_FACTOR = 500L

    fun getNextMatch(
      table: JTable,
      prefix: String,
      startRow: Int,
      bias: Bias,
    ): Int {
      val rowCount = table.rowCount
      require(startRow in 0..<rowCount) { "(0 <= startRow < rowCount) is false" }
      val upperPrefix = prefix.uppercase(Locale.ENGLISH)
      val model = table.model
      // start search from the next/previous element from the
      // selected element
      val increment = if (bias == Bias.Forward) 1 else -1
      return (0..<rowCount)
        .map { Math.floorMod(startRow + it * increment, rowCount) }
        .firstOrNull { row ->
          // the row index is a view index, convert it for the sorted table
          val value = model.getValueAt(table.convertRowIndexToModel(row), TARGET_COLUMN)
          value
            ?.toString()
            .orEmpty()
            .uppercase(Locale.ENGLISH)
            .startsWith(upperPrefix)
        }
        ?: -1
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
