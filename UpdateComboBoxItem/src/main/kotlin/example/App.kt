package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ListDataEvent
import javax.swing.plaf.basic.ComboPopup

class MainPanel : JPanel(BorderLayout()) {
  init {
    val c = GridBagConstraints()
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20))

    c.insets = Insets(10, 5, 5, 0)
    c.gridheight = 1
    c.gridwidth = 1
    c.gridy = 0

    c.gridx = 0
    c.weightx = 0.0
    c.anchor = GridBagConstraints.WEST
    listOf(
      "setSelectedIndex(-1/idx):", "contentsChanged(...):", "repaint():",
      "(remove/insert)ItemAt(...):", "fireContentsChanged(...):")
      .map { JLabel(it) }
      .forEach {
        p.add(it, c)
        c.gridy += 1
      }

    c.gridy = 0
    c.gridx = 1
    c.weightx = 1.0
    c.fill = GridBagConstraints.HORIZONTAL

    val m = arrayOf(
      CheckableItem("aaa", false),
      CheckableItem("00000", true),
      CheckableItem("111", false),
      CheckableItem("33333", true),
      CheckableItem("2222", true),
      CheckableItem("444444", false)
    )

    val combo0 = CheckedComboBox(DefaultComboBoxModel(m))
    val combo1 = CheckedComboBox1(DefaultComboBoxModel(m))
    val combo2 = CheckedComboBox2(DefaultComboBoxModel(m))
    val combo3 = CheckedComboBox3(DefaultComboBoxModel(m))
    val combo4 = CheckedComboBox4(CheckableComboBoxModel(m))

    listOf(combo0, combo1, combo2, combo3, combo4)
      .forEach {
        p.add(it, c)
        c.gridy += 1
      }

    add(p, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }
}

open class CheckableItem(private val text: String, var isSelected: Boolean) {
  override fun toString() = text
}

class CheckBoxCellRenderer<E : CheckableItem> : ListCellRenderer<E> {
  private val label = JLabel(" ")
  private val check = JCheckBox(" ")

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    if (index < 0) {
      val txt = getCheckedItemString(list.getModel())
      // label.setText(if (txt.isEmpty()) " " else txt)
      label.setText(txt.takeUnless { it.isEmpty() } ?: " ")
      return label
    } else {
      check.setText(value.toString())
      check.setSelected(value.isSelected)
      if (isSelected) {
        check.setBackground(list.getSelectionBackground())
        check.setForeground(list.getSelectionForeground())
      } else {
        check.setBackground(list.getBackground())
        check.setForeground(list.getForeground())
      }
      return check
    }
  }

  private fun <E : CheckableItem> getCheckedItemString(model: ListModel<E>): String {
    return (0 until model.getSize())
        .asSequence()
        .map { model.getElementAt(it) }
        .filter { it.isSelected }
        .map { it.toString() }
        .sorted()
        .joinToString()
  }
}

open class CheckedComboBox<E : CheckableItem> : JComboBox<E> {
  private var keepOpen = false
  @Transient
  private var listener: ActionListener? = null

  constructor() : super()

  constructor(model: ComboBoxModel<E>) : super(model)

  override fun getPreferredSize() = Dimension(200, 20)

  override fun updateUI() {
    setRenderer(null)
    removeActionListener(listener)
    super.updateUI()
    listener = ActionListener { e ->
      if (e.getModifiers() and AWTEvent.MOUSE_EVENT_MASK.toInt() != 0) {
        updateItem(getSelectedIndex())
        keepOpen = true
      }
    }
    setRenderer(CheckBoxCellRenderer<CheckableItem>())
    addActionListener(listener)
    getInputMap(JComponent.WHEN_FOCUSED)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "checkbox-select")
    getActionMap().put("checkbox-select", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val a = getAccessibleContext().getAccessibleChild(0)
        if (a is ComboPopup) {
          updateItem(a.getList().getSelectedIndex())
        }
      }
    })
  }

  open fun updateItem(index: Int) {
    if (isPopupVisible()) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      setSelectedIndex(-1)
      setSelectedItem(item)
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

class CheckedComboBox1<E : CheckableItem>(model: ComboBoxModel<E>) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible()) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      contentsChanged(ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index))
    }
  }
}

class CheckedComboBox2<E : CheckableItem>(model: ComboBoxModel<E>) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible()) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      repaint()
      val a = getAccessibleContext().getAccessibleChild(0)
      (a as? ComboPopup)?.getList()?.repaint()
    }
  }
}

class CheckedComboBox3<E : CheckableItem>(model: ComboBoxModel<E>) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible()) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      removeItemAt(index)
      insertItemAt(item, index)
      setSelectedItem(item)
    }
  }
}

// class CheckableComboBoxModel<E> @SafeVarargs constructor(vararg items: E) : DefaultComboBoxModel<E>(items) {
class CheckableComboBoxModel<E>(items: Array<E>) : DefaultComboBoxModel<E>(items) {
  fun fireContentsChanged(index: Int) {
    super.fireContentsChanged(this, index, index)
  }
}

class CheckedComboBox4<E : CheckableItem>(model: ComboBoxModel<E>) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible()) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      (getModel() as? CheckableComboBoxModel<E>)?.fireContentsChanged(index)
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
