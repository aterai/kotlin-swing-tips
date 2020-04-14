package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private val leftTextField = JTextField()
private val rightTextField = JTextField()

private fun initTextField(item: Any?) {
  (item as? PairItem)?.also {
    leftTextField.text = it.leftText
    rightTextField.text = it.rightText
  }
}

fun makeUI(): Component {
  leftTextField.isEditable = false
  rightTextField.isEditable = false
  val model = DefaultComboBoxModel<PairItem>()
  model.addElement(PairItem("aaa", "846876"))
  model.addElement(PairItem("bbb bbb", "123456"))
  model.addElement(PairItem("cc cc cc", "iop.23456789"))
  model.addElement(PairItem("dd dd dd", "64345424684"))
  model.addElement(PairItem("eee eee", "98765432210"))
  val combo = JComboBox(model)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      initTextField(e.item)
    }
  }
  initTextField(combo.getItemAt(combo.selectedIndex))
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(leftTextField)
  box.add(Box.createVerticalStrut(2))
  box.add(rightTextField)
  box.add(Box.createVerticalStrut(5))
  box.add(combo)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private data class PairItem(val leftText: String, val rightText: String) {
  private val htmlText: String
    get() =
      "<html><table width='290'><tr><td align='left'>$leftText</td><td align='right'>$rightText</td></tr></table></html>"

  override fun toString() = htmlText
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
