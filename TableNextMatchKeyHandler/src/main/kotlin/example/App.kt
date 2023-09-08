package example

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.Locale
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.text.Position.Bias

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
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

  private fun isNavigationKey(event: KeyEvent): Boolean {
    val table = event.component as? JTable ?: return false
    val inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    val key = KeyStroke.getKeyStrokeForEvent(event)
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
    val src = e.component as? JTable ?: return
    val max = src.rowCount
    if (max == 0 || e.isAltDown || isNavigationKey(e)) {
      return
    }
    val c = e.keyChar
    val increment = if (e.isShiftDown) -1 else 1
    val time = e.getWhen()
    var startIndex = src.selectedRow
    if (time - lastTime < TIME_FACTOR) {
      typedString += c
      if (prefix.length == 1 && c == prefix[0]) {
        startIndex += increment
      } else {
        prefix = typedString
      }
    } else {
      startIndex += increment
      typedString = c.toString()
      prefix = typedString
    }
    lastTime = time
    scrollNextMatch(src, max, e, prefix, startIndex)
  }

  private fun scrollNextMatch(
    src: JTable,
    max: Int,
    e: KeyEvent,
    prf: String,
    startIdx: Int
  ) {
    var start = startIdx
    var fromSelection = !src.selectionModel.isSelectionEmpty
    if (start < 0 || start >= max) {
      if (e.isShiftDown) {
        start = max - 1
      } else {
        fromSelection = false
        start = 0
      }
    }
    val bias = if (e.isShiftDown) Bias.Backward else Bias.Forward
    var index = getNextMatch(src, prf, start, bias)
    if (index >= 0) {
      src.selectionModel.setSelectionInterval(index, index)
      src.scrollRectToVisible(src.getCellRect(index, TARGET_COLUMN, true))
    } else if (fromSelection) { // wrap
      index = getNextMatch(src, prf, 0, bias)
      if (index >= 0) {
        src.selectionModel.setSelectionInterval(index, index)
        src.scrollRectToVisible(src.getCellRect(index, TARGET_COLUMN, true))
      }
    }
  }

  companion object {
    private const val TARGET_COLUMN = 0
    private const val TIME_FACTOR = 500L

    fun getNextMatch(
      table: JTable,
      prefix: String,
      startingRow: Int,
      bias: Bias
    ): Int {
      val max = table.rowCount
      require(!(startingRow < 0 || startingRow >= max)) { "(0 <= startingRow < max) is false" }
      val casePrefix = prefix.uppercase(Locale.ENGLISH)
      // start search from the next/previous element from the
      // selected element
      val increment = if (bias == Bias.Forward) 1 else -1
      var row = startingRow
      do {
        val value = table.getValueAt(row, TARGET_COLUMN)
        val text = value.toString()
        if (text.uppercase(Locale.ENGLISH).startsWith(casePrefix)) {
          return row
        }
        row = (row + increment + max) % max
      } while (row != startingRow)
      return -1
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
