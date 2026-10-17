package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.ComboPopup

private const val SELECTION_LIMIT = 3

fun createUI(): Component {
  val p = JPanel()
  for (i in 0..<8) {
    p.add(GroupCheckBox("JCheckBox$i", SELECTION_LIMIT))
  }
  p.add(GroupCheckComboBox(makeModel(), SELECTION_LIMIT))
  return JPanel(BorderLayout(5, 5)).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class GroupCheckBox(
  title: String?,
  private val maxSelectionCount: Int,
) : JCheckBox(title) {
  init {
    // Replace the model in the constructor rather than in updateUI(),
    // so that the selection state is not lost when the Look&Feel is changed.
    model = object : ToggleButtonModel() {
      override fun setSelected(selected: Boolean) {
        // Reject only a new selection that would exceed the limit
        // (re-selecting an already selected check box is not an error).
        if (selected && !isSelected && selectedObjects.size >= maxSelectionCount) {
          UIManager.getLookAndFeel().provideErrorFeedback(this@GroupCheckBox)
        } else {
          super.setSelected(selected)
        }
      }

      // Returns the selected buttons in the same parent container (the group).
      // Before being added to a container, the group consists of this check box only.
      override fun getSelectedObjects() =
        (parent?.components ?: arrayOf(this@GroupCheckBox))
          .filterIsInstance<AbstractButton>()
          .filter { it.isSelected }
          .toTypedArray()
    }
  }
}

private fun makeModel(): ComboBoxModel<CheckItem> {
  val m = arrayOf(
    CheckItem("One", false),
    CheckItem("Two", true),
    CheckItem("Three", false),
    CheckItem("Four", false),
    CheckItem("Five", false),
  )
  return DefaultComboBoxModel(m)
}

private data class CheckItem(
  val title: String,
  val isSelected: Boolean,
) {
  override fun toString() = title
}

private open class GroupCheckComboBox(
  model: ComboBoxModel<CheckItem>,
  private val groupSize: Int,
) : JComboBox<CheckItem>(model) {
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
        // The combo box display area shows the comma-separated selected items
        c.foreground = list.foreground
        (c as? JLabel)?.also {
          it.text = selectedObjects.joinToString { o -> o.toString() }
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
    if (isPopupVisible && index >= 0 && item != null) {
      if (item.isSelected) {
        removeItemAt(index)
        insertItemAt(CheckItem(item.title, false), index)
      } else {
        if (selectedObjects.size == groupSize) {
          UIManager.getLookAndFeel().provideErrorFeedback(this)
        } else {
          removeItemAt(index)
          insertItemAt(CheckItem(item.title, true), index)
        }
      }
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

  override fun getSelectedObjects() =
    (0..<itemCount)
      .toList()
      .mapNotNull { getItemAt(it) }
      .filter(CheckItem::isSelected)
      .toTypedArray()
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
