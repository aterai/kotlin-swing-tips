package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("JTextField", "JTextArea")
  val data = arrayOf(
    arrayOf("aaa", "JTextArea+JScrollPane\nCtrl-Enter: stopCellEditing"),
    arrayOf("bbb", "ccc"),
    arrayOf("11112222", "333\n444\n555"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      autoCreateRowSorter = true
      surrendersFocusOnKeystroke = true
      setRowHeight(64)
      val c = getColumnModel().getColumn(1)
      c.cellEditor = TextAreaCellEditor()
      c.cellRenderer = TextAreaCellRenderer()
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextAreaCellEditor : AbstractCellEditor(), TableCellEditor {
  private val textArea = JTextArea()
  private val scroll = JScrollPane(textArea)

  init {
    scroll.border = BorderFactory.createEmptyBorder()
    textArea.lineWrap = true
    textArea.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
    val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
    // Java 10: int modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
    val enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifiers)
    textArea.getInputMap(JComponent.WHEN_FOCUSED).put(enter, KEY)
    val action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        stopCellEditing()
      }
    }
    textArea.actionMap.put(KEY, action)
  }

  override fun getCellEditorValue() = textArea.text

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    // println("getTableCellEditorComponent")
    textArea.font = table.font
    textArea.text = value?.toString() ?: ""
    EventQueue.invokeLater {
      textArea.caretPosition = textArea.text.length
      textArea.requestFocusInWindow()
      // println("invokeLater: getTableCellEditorComponent")
    }
    return scroll
  }

  override fun isCellEditable(e: EventObject): Boolean {
    if (e is MouseEvent) {
      return e.clickCount >= 2
    }
    // println("isCellEditable")
    EventQueue.invokeLater {
      if (e is KeyEvent) {
        val kc = e.keyChar
        if (Character.isUnicodeIdentifierStart(kc)) {
          textArea.text = textArea.text + kc
          // println("invokeLater: isCellEditable")
        }
      }
    }
    return true
  }

  companion object {
    private const val KEY = "Stop-Cell-Editing"
  }
}

private class TextAreaCellRenderer : TableCellRenderer {
  private val textArea = JTextArea()

  init {
    textArea.lineWrap = true
    textArea.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (isSelected) {
      textArea.foreground = table.selectionForeground
      textArea.background = table.selectionBackground
    } else {
      textArea.foreground = table.foreground
      textArea.background = table.background
    }
    textArea.font = table.font
    textArea.text = value?.toString() ?: ""
    return textArea
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
