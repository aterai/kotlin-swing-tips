package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private val disableIndexSet: MutableSet<Int> = HashSet()
private val field = JTextField("1, 2, 5")

private fun initDisableIndex(set: MutableSet<Int>) {
  set.clear()
  runCatching {
    set.addAll(
      field.text.split(",".toRegex()).toTypedArray()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.toInt() }
        .toSet()
    )
  }.onFailure {
    Toolkit.getDefaultToolkit().beep()
    JOptionPane.showMessageDialog(field, "invalid value.\n" + it.message, "Error", JOptionPane.ERROR_MESSAGE)
  }
}

private fun makeList(disableIndexSet: Set<Int>): JList<String> {
  val model = DefaultListModel<String>()
  model.addElement("11111111111")
  model.addElement("222222222222222222")
  model.addElement("3333333333333")
  model.addElement("4444444444")
  model.addElement("5555555555555555")
  model.addElement("6666666666666")
  model.addElement("777777")

  return object : JList<String>(model) {
    override fun updateUI() {
      cellRenderer = null
      super.updateUI()
      val renderer = cellRenderer
      cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
        if (disableIndexSet.contains(index)) {
          renderer.getListCellRendererComponent(list, value, index, false, false)?.also {
            it.isEnabled = false
          }
        } else {
          renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        }
      }
    }
  }
}

fun makeUI(): Component {
  val list = makeList(disableIndexSet)
  initDisableIndex(disableIndexSet)

  val am = list.actionMap
  am.put("selectNextRow", object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = list.selectedIndex
      for (i in index + 1 until list.model.size) {
        if (!disableIndexSet.contains(i)) {
          list.selectedIndex = i
          break
        }
      }
    }
  })
  am.put("selectPreviousRow", object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val index = list.selectedIndex
      for (i in index - 1 downTo 0) {
        if (!disableIndexSet.contains(i)) {
          list.selectedIndex = i
          break
        }
      }
    }
  })

  val button = JButton("init")
  button.addActionListener {
    initDisableIndex(disableIndexSet)
    list.repaint()
  }

  val box = Box.createHorizontalBox()
  box.add(JLabel("Disabled Item Index:"))
  box.add(field)
  box.add(Box.createHorizontalStrut(2))
  box.add(button)

  val p = JPanel(BorderLayout(5, 5))
  p.add(JScrollPane(list))
  p.add(box, BorderLayout.NORTH)
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  p.preferredSize = Dimension(320, 240)
  return p
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
