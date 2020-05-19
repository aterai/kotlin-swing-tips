package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val list1 = JList(makeModel())
  list1.isEnabled = false

  val list2 = JList(makeModel())
  list2.isFocusable = false
  list2.selectionModel = object : DefaultListSelectionModel() {
    override fun isSelectedIndex(index: Int) = false
  }

  val list3 = JList(makeModel())
  val renderer = list3.cellRenderer
  list3.cellRenderer = ListCellRenderer { list, value, index, _, _ ->
    renderer.getListCellRendererComponent(list, value, index, false, false)
  }

  return JPanel(GridLayout(1, 0)).also {
    it.add(JScrollPane(list1))
    it.add(JScrollPane(list2))
    it.add(JScrollPane(list3))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultListModel<String>().also {
  it.addElement("0000000000000")
  it.addElement("11111")
  it.addElement("222")
  it.addElement("333333333333")
  it.addElement("444444444")
  it.addElement("55555555555")
  it.addElement("666666")
  it.addElement("7777777777")
  it.addElement("88888888888888")
  it.addElement("99")
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
