package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultEditorKit.CopyAction
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf(
    arrayOf("aaa", "bb bb bb bb", "ccc ccc"),
    arrayOf("bbb", "ff", "ggg oo pp"),
    arrayOf("CCC", "kkk", "jj"),
    arrayOf("DDD", "ii mm nn", "hhh hhh lll")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int): Class<*> {
      return getValueAt(0, column).javaClass
    }
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
  table.setDefaultRenderer(Any::class.java) { tbl, value, isSelected, _, row, column ->
    r.getTableCellRendererComponent(tbl, value, isSelected, false, row, column)
  }

  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(table))
  p.add(JScrollPane(JTextArea()))
  p.preferredSize = Dimension(320, 240)
  return p
}

private class TextComponentPopupMenu : JPopupMenu() {
  private val copyAction = CopyAction()
  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTextComponent) {
      copyAction.isEnabled = c.selectedText != null
      super.show(c, x, y)
    }
  }

  init {
    add(copyAction)
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
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
