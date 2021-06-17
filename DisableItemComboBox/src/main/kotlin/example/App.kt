package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val field = JTextField("1, 2, 5")
  val combo = DisableItemComboBox(makeModel()).also {
    it.setDisableIndexSet(getDisableIndexFromTextField(field))
  }
  val button = JButton("init").also {
    it.addActionListener { combo.setDisableIndexSet(getDisableIndexFromTextField(field)) }
  }
  val box = Box.createHorizontalBox().also {
    it.add(JLabel("Disabled Item Index:"))
    it.add(field)
    it.add(Box.createHorizontalStrut(2))
    it.add(button)
  }
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)
    it.add(combo, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("0000000000000")
  it.addElement("111111")
  it.addElement("222222222222")
  it.addElement("33")
  it.addElement("4444444444444444")
  it.addElement("555555555555555555555555")
  it.addElement("6666666666")
}

private fun getDisableIndexFromTextField(field: JTextField) = runCatching {
  field.text.split(",")
    .map { it.trim() }
    .filterNot { it.isEmpty() }
    .map { it.toInt() }
    .toSet()
}.onFailure {
  Toolkit.getDefaultToolkit().beep()
  val p = field.rootPane
  JOptionPane.showMessageDialog(p, "invalid value.\n${it.message}", "Error", JOptionPane.ERROR_MESSAGE)
}.getOrNull().orEmpty()

private class DisableItemComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  private val disableIndexSet = mutableSetOf<Int>()
  private var isDisableIndex = false
  private val up = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      for (i in selectedIndex - 1 downTo 0) {
        if (isEnabledIndex(i)) {
          selectedIndex = i
          break
        }
      }
    }
  }
  private val down = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      for (i in selectedIndex + 1 until getModel().size) {
        if (isEnabledIndex(i)) {
          selectedIndex = i
          break
        }
      }
    }
  }

  // constructor() : super()

  // constructor(model: ComboBoxModel<E>) : super(model)

  // constructor(items: Array<E>) : super(items)

  override fun updateUI() {
    super.updateUI()
    val renderer = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      val f = isEnabledIndex(index)
      renderer.getListCellRendererComponent(list, value, index, isSelected and f, cellHasFocus and f).also {
        it.isEnabled = f
      }
    }
    EventQueue.invokeLater {
      val prevKey = "selectPrevious3"
      val nextKey = "selectNext3"
      val am = actionMap
      am.put(prevKey, up)
      am.put(nextKey, down)
      val im = inputMap
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), prevKey)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), prevKey)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), nextKey)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), nextKey)
    }
  }

  override fun setPopupVisible(v: Boolean) {
    if (!v && isDisableIndex) {
      isDisableIndex = false
    } else {
      super.setPopupVisible(v)
    }
  }

  override fun setSelectedIndex(index: Int) {
    if (isEnabledIndex(index)) {
      // isDisableIndex = false
      super.setSelectedIndex(index)
    } else {
      isDisableIndex = true
    }
  }

  fun isEnabledIndex(idx: Int) = !disableIndexSet.contains(idx)

  fun setDisableIndexSet(set: Set<Int>) {
    disableIndexSet.clear()
    disableIndexSet.addAll(set)
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
