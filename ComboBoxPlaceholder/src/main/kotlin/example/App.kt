package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val arrays = arrayOf(
    arrayOf("blue", "violet", "red", "yellow"),
    arrayOf("basketball", "soccer", "football", "hockey"),
    arrayOf("hot dogs", "pizza", "ravioli", "bananas")
  )
  val combo1 = JComboBox(arrayOf("colors", "sports", "food"))
  val combo2 = JComboBox<String>()
  combo1.selectedIndex = -1
  combo1.renderer = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val str = value?.toString() ?: "- Select category -"
      super.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus)
      return this
    }
  }
  combo1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val idx = (e.itemSelectable as JComboBox<*>).selectedIndex
      combo2.model = DefaultComboBoxModel(arrays[idx])
      combo2.selectedIndex = -1
    }
  }

  combo2.renderer = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val str = value?.toString() ?: "- Select type -"
      super.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus)
      return this
    }
  }

  val p = JPanel(GridLayout(4, 1, 5, 5))
  p.add(JLabel("Category"))
  p.add(combo1)
  p.add(JLabel("Type"))
  p.add(combo2)

  val button = JButton("clear")
  button.addActionListener {
    combo1.selectedIndex = -1
    combo2.setModel(DefaultComboBoxModel())
  }

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
    it.add(p, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
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
