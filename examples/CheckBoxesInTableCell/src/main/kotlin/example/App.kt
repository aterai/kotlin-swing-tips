package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun createUI(): Component {
  val columnNames = arrayOf("user", "rwx")
  val data = arrayOf<Array<Any>>(
    arrayOf("owner", 7),
    arrayOf("group", 6),
    arrayOf("other", 5),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val c = getColumnModel().getColumn(1)
      c.cellRenderer = CheckBoxesRenderer()
      c.cellEditor = CheckBoxesEditor()
      putClientProperty("terminateEditOnFocusLost", true)
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private open class CheckBoxesPanel : JPanel() {
  private val checkBoxes = SYMBOLS.map { makeCheckBox(it) }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    background = TRANSPARENT
    layout = BoxLayout(this, BoxLayout.X_AXIS)
  }

  // Re-add the check boxes on every update to avoid ghost images on Windows Aero
  private fun initCheckBoxes() {
    removeAll()
    checkBoxes.forEach {
      add(it)
      add(Box.createHorizontalStrut(5))
    }
  }

  fun updateCheckBoxes(value: Any?) {
    initCheckBoxes()
    val mode = value as? Int ?: 0
    checkBoxes.forEachIndexed { i, b -> b.isSelected = mode and getBit(i) != 0 }
  }

  fun toggleCheckBox(index: Int) {
    checkBoxes[index].doClick()
  }

  fun getMode() = checkBoxes.indices
    .filter { checkBoxes[it].isSelected }
    .fold(0) { acc, i -> acc or getBit(i) }

  companion object {
    // Permission symbols in "rwx" display order; bit: r -> 4, w -> 2, x -> 1
    val SYMBOLS = listOf("r", "w", "x")
    private val TRANSPARENT = Color(0x0, true)

    private fun makeCheckBox(title: String) = JCheckBox(title).also {
      it.isOpaque = false
      it.isFocusable = false
      it.isRolloverEnabled = false
      it.background = TRANSPARENT
    }

    // Convert an index in SYMBOLS to its chmod bit: 0 -> 4, 1 -> 2, 2 -> 1
    private fun getBit(index: Int) = 1 shl (SYMBOLS.size - 1 - index)
  }
}

private class CheckBoxesRenderer : TableCellRenderer {
  private val renderer = CheckBoxesPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    renderer.updateCheckBoxes(value)
    return renderer
  }
}

private class CheckBoxesEditor :
  AbstractCellEditor(),
  TableCellEditor {
  private val editor = CheckBoxesPanel()

  init {
    val am = editor.actionMap
    val im = editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    CheckBoxesPanel.SYMBOLS.forEachIndexed { i, symbol ->
      am.put(symbol, createToggleAction(i))
      // "r" -> KeyEvent.VK_R, "w" -> KeyEvent.VK_W, "x" -> KeyEvent.VK_X
      val keyCode = KeyEvent.getExtendedKeyCodeForChar(symbol[0].code)
      im.put(KeyStroke.getKeyStroke(keyCode, 0), symbol)
    }
  }

  private fun createToggleAction(index: Int) = object : AbstractAction(CheckBoxesPanel.SYMBOLS[index]) {
    override fun actionPerformed(e: ActionEvent) {
      editor.toggleCheckBox(index)
      fireEditingStopped()
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    editor.updateCheckBoxes(value)
    return editor
  }

  override fun getCellEditorValue(): Any = editor.getMode()
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
