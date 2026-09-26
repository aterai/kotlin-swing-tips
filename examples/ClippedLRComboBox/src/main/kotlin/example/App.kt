package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*

fun createUI(): Component {
  val combo0 = JComboBox(createModel())
  val box0 = createTitledBox("DefaultComboBox", combo0)

  val combo1 = object : JComboBox<PairItem>(createModel()) {
    override fun updateUI() {
      // setRenderer(null)
      super.updateUI()
      setRenderer(MultiColumnCellRenderer())
    }
  }
  val box1 = createTitledBox("MultiColumnComboBox", combo1)
  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.NORTH)
    it.add(box0, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTitledBox(
  title: String,
  combo: JComboBox<*>,
): Box {
  val leftTextField = JTextField()
  val rightTextField = JTextField()
  leftTextField.isEditable = false
  rightTextField.isEditable = false
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder(title)
  box.add(Box.createVerticalStrut(2))
  box.add(combo)
  box.add(Box.createVerticalStrut(2))
  box.add(leftTextField)
  box.add(Box.createVerticalStrut(2))
  box.add(rightTextField)
  combo.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is PairItem) {
      updateTextFields(item, leftTextField, rightTextField)
    }
  }
  updateTextFields(combo.selectedItem as? PairItem, leftTextField, rightTextField)
  return box
}

private fun updateTextFields(
  item: PairItem?,
  left: JTextField,
  right: JTextField,
) {
  left.text = item?.leftText ?: ""
  right.text = item?.rightText ?: ""
}

private fun createModel() = DefaultComboBoxModel<PairItem>().also {
  val name = "loooooooooooooooooooooooooooooooooong.1234567890.1234567890"
  it.addElement(PairItem("ccc", "846876"))
  it.addElement(PairItem("bbb", "111111111111111111111"))
  it.addElement(PairItem(name, "aaa.1234567890.1234567890.1234567890"))
  it.addElement(PairItem("14234125", "64345424543523452345234523684"))
  it.addElement(PairItem("555555", "addElement"))
  it.addElement(PairItem("666666666", "ddd"))
  it.addElement(PairItem("7777777", "33333"))
  it.addElement(PairItem("88888888", "4444444444"))
}

private class MultiColumnCellRenderer : ListCellRenderer<PairItem> {
  private val leftLabel = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      isOpaque = false
      border = BorderFactory.createEmptyBorder(0, 2, 0, 0)
    }
  }
  private val rightLabel = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      isOpaque = false
      border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
      horizontalAlignment = RIGHT
    }

    override fun getPreferredSize() = Dimension(80, 0)
  }
  private val renderer = object : JPanel(BorderLayout()) {
    // override fun getPreferredSize(): Dimension {
    //   val d = super.getPreferredSize()
    //   return Dimension(0, d.height)
    // }
    override fun getPreferredSize() = super.getPreferredSize()?.also { it.width = 0 }

    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    }
  }

  init {
    renderer.add(leftLabel)
    renderer.add(rightLabel, BorderLayout.EAST)
  }

  override fun getListCellRendererComponent(
    list: JList<out PairItem>,
    value: PairItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    leftLabel.text = value?.leftText ?: ""
    rightLabel.text = value?.rightText ?: ""
    leftLabel.font = list.font
    rightLabel.font = list.font
    val fgc: Color
    val bgc: Color
    if (index >= 0 && isSelected) {
      fgc = list.selectionForeground
      bgc = list.selectionBackground
    } else {
      fgc = list.foreground
      bgc = list.background
    }
    leftLabel.foreground = fgc
    rightLabel.foreground = blend(fgc, bgc)
    renderer.background = bgc
    renderer.isOpaque = index >= 0
    return renderer
  }

  private fun blend(
    c1: Color,
    c2: Color,
  ): Color {
    val r = (c1.red + c2.red) / 2
    val g = (c1.green + c2.green) / 2
    val b = (c1.blue + c2.blue) / 2
    return Color(r, g, b)
  }
}

private data class PairItem(
  val leftText: String,
  val rightText: String,
) {
  override fun toString() = "$leftText / $rightText"
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
