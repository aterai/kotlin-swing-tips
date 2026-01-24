package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf(
    arrayOf("aaa", "bb bb bb bb", "ccc ccc"),
    arrayOf("bbb", "ff", "ggg oo pp"),
    arrayOf("CCC", "kkk", "jj"),
    arrayOf("DDD", "ii mm nn", "hhh hhh lll"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.selectionForeground = Color.BLACK
  table.selectionBackground = Color(0xEE_EE_EE)

  val field = JTextField()
  field.isEditable = false
  field.background = table.selectionBackground
  field.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  field.componentPopupMenu = TextComponentPopupMenu()

  val cellEditor = DefaultCellEditor(field)
  cellEditor.clickCountToStart = 1
  table.setDefaultEditor(Any::class.java, cellEditor)

  val r = DefaultTableCellRenderer()
  table.setDefaultRenderer(Any::class.java) { t, v, selected, _, row, col ->
    r.getTableCellRendererComponent(t, v, selected, false, row, col)
  }

  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(table))
  p.add(JScrollPane(JTextArea()))
  p.preferredSize = Dimension(320, 240)
  return p
}

private class TextComponentPopupMenu : JPopupMenu() {
  private val copyAction = DefaultEditorKit.CopyAction()

  init {
    add(copyAction)
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTextComponent) {
      copyAction.isEnabled = c.selectedText != null
      super.show(c, x, y)
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
