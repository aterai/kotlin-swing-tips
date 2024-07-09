package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1))
  p.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
  p.add(JLabel("Default:"))
  p.add(JComboBox(makeModel()))
  p.add(Box.createVerticalStrut(20))
  p.add(JLabel("CheckedComboBox:"))
  p.add(CheckedComboBox(makeModel()))
  p.add(Box.createVerticalStrut(20))
  p.add(JLabel("CheckedComboBox(Windows):"))
  p.add(WindowsCheckedComboBox(makeModel()))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ComboBoxModel<CheckBoxItem> {
  val m = arrayOf(
    CheckBoxItem("aaa", false),
    CheckBoxItem("bb", true),
    CheckBoxItem("111", false),
    CheckBoxItem("33333", true),
    CheckBoxItem("2222", true),
    CheckBoxItem("c", false),
  )
  return DefaultComboBoxModel(m)
}

private data class CheckBoxItem(
  val title: String,
  val isSelected: Boolean,
) {
  override fun toString() = title
}

private open class CheckedComboBox(
  model: ComboBoxModel<CheckBoxItem>,
) : JComboBox<CheckBoxItem>(model) {
  protected var keepOpen = false
  private val panel = JPanel(BorderLayout())

  override fun getPreferredSize() = Dimension(200, 20)

  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    val a = getAccessibleContext().getAccessibleChild(0)
    if (a is ComboPopup) {
      a.list.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          val list = e.component as? JList<*> ?: return
          if (SwingUtilities.isLeftMouseButton(e)) {
            keepOpen = true
            updateItem(list.locationToIndex(e.point))
          }
        }
      })
    }
    val renderer = DefaultListCellRenderer()
    val check = JCheckBox()
    check.isOpaque = false
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      panel.removeAll()
      val c = renderer.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      )
      if (index < 0) {
        c.foreground = list.foreground
        (c as? JLabel)?.also {
          it.text = getCheckedItemString(list.model).ifEmpty { " " }
          it.isOpaque = false
        }
        panel.isOpaque = false
      } else {
        check.isSelected = value.isSelected
        panel.add(check, BorderLayout.WEST)
        panel.isOpaque = true
        panel.background = c.background
      }
      panel.add(c)
      panel
    }
    initActionMap()
  }

  protected fun initActionMap() {
    getInputMap(JComponent.WHEN_FOCUSED).put(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
      "checkbox-select",
    )
    val action = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.also {
          updateItem(it.list.selectedIndex)
        }
      }
    }
    actionMap.put("checkbox-select", action)
  }

  protected fun updateItem(index: Int) {
    val item = getItemAt(index)
    if (isPopupVisible && item != null) {
      removeItemAt(index)
      insertItemAt(CheckBoxItem(item.title, !item.isSelected), index)
      selectedIndex = index
    }
  }

  override fun setPopupVisible(v: Boolean) {
    if (keepOpen) {
      keepOpen = false
    } else {
      super.setPopupVisible(v)
    }
  }

  protected fun getCheckedItemString(
    model: ListModel<out CheckBoxItem>,
  ) = (0..<model.size)
    .asSequence()
    .map { model.getElementAt(it) }
    .filter { it.isSelected }
    .map { it.toString() }
    .sorted()
    .joinToString()
}

private class WindowsCheckedComboBox(
  model: ComboBoxModel<CheckBoxItem>,
) : CheckedComboBox(model) {
  private var listener: ActionListener? = null

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
    addActionListener(listener)

    val label = JLabel(" ")
    val check = JCheckBox(" ")
    setRenderer { list, value, index, isSelected, _ ->
      if (index < 0) {
        label.text = getCheckedItemString(list.model).ifEmpty { " " }
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
    initActionMap()
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
