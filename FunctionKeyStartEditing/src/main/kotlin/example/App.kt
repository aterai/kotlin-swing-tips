package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val check = JCheckBox("ignore: F1,F4-F7,F9-", true)

  val textarea = JTextArea("F2: startEditing\nF8: focusHeader\nF3: beep")
  textarea.isEditable = false

  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )

  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun editCellAt(
      row: Int,
      column: Int,
      e: EventObject,
    ) = !(check.isSelected && isFunctionKey(e)) && super.editCellAt(row, column, e)
  }
  table.autoCreateRowSorter = true

  val act = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      // println("F3")
      Toolkit.getDefaultToolkit().beep()
    }
  }
  table.actionMap.put("beep", act)

  val im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "beep")

  val p = JPanel(BorderLayout())
  p.add(check, BorderLayout.NORTH)
  p.add(JScrollPane(textarea))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun isFunctionKey(e: EventObject) =
  e is KeyEvent && e.keyCode in KeyEvent.VK_F1..KeyEvent.VK_F24

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
