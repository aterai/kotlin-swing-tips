package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*

fun makeUI(): Component {
  val combo = object : JComboBox<PairItem>(makeModel()) {
    override fun updateUI() {
      // setRenderer(null)
      super.updateUI()
      setRenderer(MultiColumnCellRenderer())
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(makeTitledBox("MultiColumnComboBox", combo), BorderLayout.NORTH)
    it.add(makeTitledBox("DefaultComboBox", JComboBox(makeModel())), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledBox(
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
      leftTextField.text = item.leftText
      rightTextField.text = item.rightText
    }
  }
  return box
}

private fun makeModel() = DefaultComboBoxModel<PairItem>().also {
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
      foreground = Color.GRAY
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

  override fun getListCellRendererComponent(
    list: JList<out PairItem>,
    value: PairItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    leftLabel.text = value.leftText
    rightLabel.text = value.rightText

    leftLabel.font = list.font
    rightLabel.font = list.font

    renderer.add(leftLabel)
    renderer.add(rightLabel, BorderLayout.EAST)

    if (index < 0) {
      leftLabel.foreground = list.foreground
      renderer.isOpaque = false
    } else {
      if (isSelected) {
        leftLabel.foreground = list.selectionForeground
        renderer.background = list.selectionBackground
      } else {
        leftLabel.foreground = list.foreground
        renderer.background = list.background
      }
      renderer.isOpaque = true
    }
    return renderer
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
