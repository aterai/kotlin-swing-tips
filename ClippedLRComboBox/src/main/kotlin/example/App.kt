package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val combo = object : JComboBox<PairItem>(makeModel()) {
      override fun updateUI() {
        // setRenderer(null);
        super.updateUI()
        setRenderer(MultiColumnCellRenderer())
      }
    }
    add(makeTitledBox("MultiColumnComboBox", combo), BorderLayout.NORTH)
    add(makeTitledBox("DefaultComboBox", JComboBox<PairItem>(makeModel())), BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledBox(title: String, combo: JComboBox<*>): Box {
    val leftTextField = JTextField()
    val rightTextField = JTextField()
    leftTextField.setEditable(false)
    rightTextField.setEditable(false)
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createTitledBorder(title))
    box.add(Box.createVerticalStrut(2))
    box.add(combo)
    box.add(Box.createVerticalStrut(2))
    box.add(leftTextField)
    box.add(Box.createVerticalStrut(2))
    box.add(rightTextField)
    combo.addItemListener { e ->
      val item = e.getItem()
      if (e.getStateChange() == ItemEvent.SELECTED && item is PairItem) {
        leftTextField.setText(item.leftText)
        rightTextField.setText(item.rightText)
      }
    }
    return box
  }

  private fun makeModel() = DefaultComboBoxModel<PairItem>().also {
    val name = "loooooooooooooooooooooooooooooooooong.1234567890.1234567890"
    it.addElement(PairItem("asdfasdf", "846876"))
    it.addElement(PairItem("bxcvzx", "asdfaasdfasdfasdfasdfsasd"))
    it.addElement(PairItem(name, "qwerqwer.1234567890.1234567890.1234567890"))
    it.addElement(PairItem("14234125", "64345424543523452345234523684"))
    it.addElement(PairItem("hjklhjk", "addElement"))
    it.addElement(PairItem("aaaaaaaa", "ddd"))
    it.addElement(PairItem("bbbbbbbb", "eeeee"))
    it.addElement(PairItem("cccccccc", "fffffff"))
  }
}

internal class MultiColumnCellRenderer : ListCellRenderer<PairItem> {
  private val leftLabel = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      setOpaque(false)
      setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0))
    }
  }
  private val rightLabel = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      setOpaque(false)
      setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2))
      setForeground(Color.GRAY)
      setHorizontalAlignment(SwingConstants.RIGHT)
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
      setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out PairItem>,
    value: PairItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    leftLabel.setText(value.leftText)
    rightLabel.setText(value.rightText)

    leftLabel.setFont(list.getFont())
    rightLabel.setFont(list.getFont())

    renderer.add(leftLabel)
    renderer.add(rightLabel, BorderLayout.EAST)

    if (index < 0) {
      leftLabel.setForeground(list.getForeground())
      renderer.setOpaque(false)
    } else {
      leftLabel.setForeground(if (isSelected) list.getSelectionForeground() else list.getForeground())
      renderer.setBackground(if (isSelected) list.getSelectionBackground() else list.getBackground())
      renderer.setOpaque(true)
    }
    return renderer
  }
}

data class PairItem(val leftText: String, val rightText: String) {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
