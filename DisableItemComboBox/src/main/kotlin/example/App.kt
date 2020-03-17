package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val field = JTextField("1, 2, 5")

    val combo = DisableItemComboBox(makeModel())
    combo.setDisableIndex(getDisableIndexFromTextField(field))

    val button = JButton("init")
    button.addActionListener { combo.setDisableIndex(getDisableIndexFromTextField(field)) }

    val box = Box.createHorizontalBox()
    box.add(JLabel("Disabled Item Index:"))
    box.add(field)
    box.add(Box.createHorizontalStrut(2))
    box.add(button)
    add(box, BorderLayout.SOUTH)
    add(combo, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    setPreferredSize(Dimension(320, 240))
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
    field.getText().split(",")
      .map { it.trim() }
      .filterNot { it.isEmpty() }
      .map { it.toInt() }
      .toSet()
  }.onFailure {
    Toolkit.getDefaultToolkit().beep()
    val root = field.getRootPane()
    JOptionPane.showMessageDialog(root, "invalid value.\n${it.message}", "Error", JOptionPane.ERROR_MESSAGE)
  }.getOrNull().orEmpty()
}

class DisableItemComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  private val disableIndexSet = mutableSetOf<Int>()
  private var isDisableIndex = false
  private val up = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val si = getSelectedIndex()
      for (i in si - 1 downTo 0) {
        if (!disableIndexSet.contains(i)) {
          setSelectedIndex(i)
          break
        }
      }
    }
  }
  private val down = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val si = getSelectedIndex()
      for (i in si + 1 until getModel().getSize()) {
        if (!disableIndexSet.contains(i)) {
          setSelectedIndex(i)
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
      val c: Component
      if (disableIndexSet.contains(index)) {
        c = renderer.getListCellRendererComponent(list, value, index, false, false)
        c.setEnabled(false)
      } else {
        c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        c.setEnabled(true)
      }
      return@setRenderer c
    }
    EventQueue.invokeLater {
      val am = getActionMap()
      am.put("selectPrevious3", up)
      am.put("selectNext3", down)
      val im = getInputMap()
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "selectPrevious3")
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), "selectPrevious3")
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "selectNext3")
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), "selectNext3")
    }
  }

  fun setDisableIndex(set: Set<Int>) {
    disableIndexSet.clear()
    for (i in set) {
      disableIndexSet.add(i)
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
    if (disableIndexSet.contains(index)) {
      isDisableIndex = true
    } else {
      // isDisableIndex = false;
      super.setSelectedIndex(index)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
