package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.ComboPopup

class MainPanel : JPanel(BorderLayout()) {
  init {
    val m = arrayOf(
      CheckableItem("aaa", false),
      CheckableItem("bb", true),
      CheckableItem("111", false),
      CheckableItem("33333", true),
      CheckableItem("2222", true),
      CheckableItem("c", false)
    )
    val p = JPanel(GridLayout(0, 1))
    p.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
    p.add(JLabel("Default:"))
    p.add(JComboBox(m))
    p.add(Box.createVerticalStrut(20))
    p.add(JLabel("CheckedComboBox:"))
    p.add(CheckedComboBox(DefaultComboBoxModel(m)))
    add(p, BorderLayout.NORTH)
    preferredSize = Dimension(320, 240)
  }
}

data class CheckableItem(private val text: String, var isSelected: Boolean) {
  override fun toString() = text
}

// class CheckBoxCellRenderer : ListCellRenderer<CheckableItem> {
//  private val label = JLabel(" ")
//  private val check = JCheckBox(" ")
//  override fun getListCellRendererComponent(
//    list: JList<out CheckableItem>,
//    value: CheckableItem?,
//    index: Int,
//    isSelected: Boolean,
//    cellHasFocus: Boolean
//  ): Component {
//    return if (index < 0) {
//      val txt = getCheckedItemString(list.model)
//      label.text = if (txt.isEmpty()) " " else txt
//      label
//    } else {
//      check.text = value?.toString() ?: ""
//      check.isSelected = value?.isSelected == true
//      if (isSelected) {
//        check.background = list.selectionBackground
//        check.foreground = list.selectionForeground
//      } else {
//        check.background = list.background
//        check.foreground = list.foreground
//      }
//      check
//    }
//  }
//
//  private fun getCheckedItemString(model: ListModel<out CheckableItem>): String {
//    return (0 until model.size)
//      .asSequence()
//      .map { model.getElementAt(it) }
//      .filter { it.isSelected }
//      .map { it.toString() }
//      .sorted()
//      .joinToString { "," }
//  }
// }

class CheckedComboBox(model: ComboBoxModel<CheckableItem>) : JComboBox<CheckableItem>(model) {
  private var keepOpen = false
  @Transient
  private var listener: ActionListener? = null

  override fun getPreferredSize() = Dimension(200, 20)

  override fun updateUI() {
    setRenderer(null)
    removeActionListener(listener)
    super.updateUI()
    listener = ActionListener { e ->
      if (e.modifiers.toLong() and AWTEvent.MOUSE_EVENT_MASK != 0L) {
        updateItem(selectedIndex)
        keepOpen = true
      }
    }
    val label = JLabel(" ")
    val check = JCheckBox(" ")
    setRenderer { list, value, index, isSelected, _ ->
      return@setRenderer if (index < 0) {
        val txt = getCheckedItemString(list.model)
        label.text = if (txt.isEmpty()) " " else txt
        label
      } else {
        check.text = value?.toString() ?: ""
        check.isSelected = value?.isSelected == true
        if (isSelected) {
          check.background = list.selectionBackground
          check.foreground = list.selectionForeground
        } else {
          check.background = list.background
          check.foreground = list.foreground
        }
        check
      }
    }
    addActionListener(listener)
    getInputMap(JComponent.WHEN_FOCUSED).put(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
      "checkbox-select"
    )
    actionMap.put("checkbox-select", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.also {
          updateItem(it.list.selectedIndex)
        }
      }
    })
  }

  private fun getCheckedItemString(model: ListModel<out CheckableItem>): String {
    return (0 until model.size)
      .asSequence()
      .map { model.getElementAt(it) }
      .filter { it.isSelected }
      .map { it.toString() }
      .sorted()
      .joinToString()
  }

  private fun updateItem(index: Int) {
    if (isPopupVisible) {
      getItemAt(index)?.also {
        it.isSelected = !it.isSelected
        selectedIndex = -1
        selectedItem = it
      }
    }
  }

  override fun setPopupVisible(v: Boolean) {
    if (keepOpen) {
      keepOpen = false
    } else {
      super.setPopupVisible(v)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
