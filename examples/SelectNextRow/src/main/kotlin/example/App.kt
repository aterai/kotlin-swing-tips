package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  val im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  val tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
  val enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
  val orgTabAction = im[tab]
  val checkBox = JCheckBox("selectNextRow: VK_TAB", true)
  checkBox.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    im.put(tab, if (flg) im[enter] else orgTabAction)
  }
  im.put(tab, im[enter])
  val shiftTab = KeyStroke.getKeyStroke("shift TAB")
  val shiftEnter = KeyStroke.getKeyStroke("shift ENTER")
  im.put(shiftTab, im[shiftEnter])
  return JPanel(BorderLayout()).also {
    it.add(checkBox, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
