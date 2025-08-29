package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
  val c = GridBagConstraints()
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createEmptyBorder(10, 20, 10, 20)

  c.insets = Insets(10, 5, 5, 0)
  c.gridheight = 1
  c.gridwidth = 1
  c.gridy = 0

  c.gridx = 0
  c.weightx = 0.0
  c.anchor = GridBagConstraints.WEST
  listOf(
    "setSelectedIndex(-1/idx):",
    "contentsChanged(...):",
    "repaint():",
    "(remove/insert)ItemAt(...):",
    "fireContentsChanged(...):",
  ).map { JLabel(it) }
    .forEach {
      p.add(it, c)
      c.gridy += 1
    }

  c.gridy = 0
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL

  val m = arrayOf(
    CheckBoxItem("aaa", false),
    CheckBoxItem("00000", true),
    CheckBoxItem("111", false),
    CheckBoxItem("33333", true),
    CheckBoxItem("2222", true),
    CheckBoxItem("444444", false),
  )

  val combo0 = CheckedComboBox(DefaultComboBoxModel(m))
  val combo1 = CheckedComboBox1(DefaultComboBoxModel(m))
  val combo2 = CheckedComboBox2(DefaultComboBoxModel(m))
  val combo3 = CheckedComboBox3(DefaultComboBoxModel(m))
  val combo4 = CheckedComboBox4(CheckComboBoxModel(m))

  listOf(combo0, combo1, combo2, combo3, combo4)
    .forEach {
      p.add(it, c)
      c.gridy += 1
    }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class CheckBoxItem(
  private val text: String,
  var isSelected: Boolean,
) {
  override fun toString() = text
}

private class CheckBoxCellRenderer<E : CheckBoxItem> : ListCellRenderer<E> {
  private val label = JLabel(" ")
  private val check = JCheckBox(" ")

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component = if (index < 0) {
    label.also {
      // val txt = getCheckedItemString(list.model)
      // it.setText(if (txt.isEmpty()) " " else txt)
      // it.text = txt.takeUnless { txt -> txt.isEmpty() } ?: " "
      it.text = getCheckedItemString(list.model).ifEmpty { " " }
    }
  } else {
    check.also {
      it.text = value.toString()
      it.isSelected = value.isSelected
      if (isSelected) {
        it.background = list.selectionBackground
        it.foreground = list.selectionForeground
      } else {
        it.background = list.background
        it.foreground = list.foreground
      }
    }
  }

  private fun <E : CheckBoxItem> getCheckedItemString(model: ListModel<E>) =
    (0..<model.size)
      .asSequence()
      .map { model.getElementAt(it) }
      .filter { it.isSelected }
      .map { it.toString() }
      .sorted()
      .joinToString()
}

private open class CheckedComboBox<E : CheckBoxItem>(
  model: ComboBoxModel<E>,
) : JComboBox<E>(model) {
  private var keepOpen = false
  private var listener: ActionListener? = null

  // constructor() : super()

  override fun getPreferredSize() = Dimension(200, 20)

  override fun updateUI() {
    setRenderer(null)
    removeActionListener(listener)
    super.updateUI()
    listener = ActionListener { e ->
      if (e.modifiers and AWTEvent.MOUSE_EVENT_MASK.toInt() != 0) {
        updateItem(selectedIndex)
        keepOpen = true
      }
    }
    setRenderer(CheckBoxCellRenderer<CheckBoxItem>())
    addActionListener(listener)

    val im = getInputMap(WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "checkbox-select")

    val action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val a = getAccessibleContext().getAccessibleChild(0)
        if (a is ComboPopup) {
          updateItem(a.list.selectedIndex)
        }
      }
    }
    actionMap.put("checkbox-select", action)
  }

  open fun updateItem(index: Int) {
    if (isPopupVisible) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      selectedIndex = -1
      selectedItem = item
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

private class CheckedComboBox1<E : CheckBoxItem>(
  model: ComboBoxModel<E>,
) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      val e = ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index)
      contentsChanged(e)
    }
  }
}

private class CheckedComboBox2<E : CheckBoxItem>(
  model: ComboBoxModel<E>,
) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      repaint()
      val a = getAccessibleContext().getAccessibleChild(0)
      (a as? ComboPopup)?.list?.repaint()
    }
  }
}

private class CheckedComboBox3<E : CheckBoxItem>(
  model: ComboBoxModel<E>,
) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      removeItemAt(index)
      insertItemAt(item, index)
      selectedItem = item
    }
  }
}

private class CheckComboBoxModel<E>(
  items: Array<E>,
) : DefaultComboBoxModel<E>(items) {
  fun fireContentsChanged(index: Int) {
    super.fireContentsChanged(this, index, index)
  }
}

private class CheckedComboBox4<E : CheckBoxItem>(
  model: ComboBoxModel<E>,
) : CheckedComboBox<E>(model) {
  override fun updateItem(index: Int) {
    if (isPopupVisible) {
      val item = getItemAt(index)
      item.isSelected = !item.isSelected
      (model as? CheckComboBoxModel<E>)?.fireContentsChanged(index)
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
