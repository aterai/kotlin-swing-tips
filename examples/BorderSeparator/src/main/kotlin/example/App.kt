package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border

fun makeUI(): Component {
  val model = makeModel()
  val box1 = Box.createVerticalBox().also {
    it.border = BorderFactory.createTitledBorder("setEditable(false)")
    it.add(makeComboBox(model))
  }
  val box2 = Box.createVerticalBox().also {
    it.border = BorderFactory.createTitledBorder("setEditable(true)")
    val combo = makeComboBox(model)
    combo.isEditable = true
    it.add(combo)
  }
  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(50, 5, 50, 5)
    it.add(box1, BorderLayout.NORTH)
    it.add(box2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultComboBoxModel<ListItem> {
  val model = DefaultComboBoxModel<ListItem>()
  model.addElement(ListItem("1111"))
  model.addElement(ListItem("1111222"))
  model.addElement(ListItem("111122233"))
  model.addElement(ListItem("444444", true))
  model.addElement(ListItem("555"))
  model.addElement(ListItem("6666666"))
  return model
}

private fun makeComboBox(
  model: ComboBoxModel<ListItem>,
) = object : JComboBox<ListItem>(model) {
  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    val renderer = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer
        .getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          if (it is JComponent && value != null) {
            it.border = ListItem.getSeparatorBorder(value, index)
          }
        }
    }
  }
}

private data class ListItem(
  val item: String,
  val hasSeparator: Boolean = false,
) {
  override fun toString() = item

  companion object {
    fun getSeparatorBorder(
      item: ListItem,
      index: Int,
    ): Border = if (index != -1 && item.hasSeparator) {
      BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY)
    } else {
      BorderFactory.createEmptyBorder()
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
