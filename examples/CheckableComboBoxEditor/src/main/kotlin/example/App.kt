package example

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*

fun createUI(): Component {
  val box = Box.createVerticalBox()
  val combo0 = JComboBox(createModel()).also {
    it.renderer = CheckComboBoxRenderer()
  }
  val title0 = "setEditable(false), setRenderer(...)"
  box.add(createTitledPanel(title0, combo0))
  box.add(Box.createVerticalStrut(5))

  val combo1 = JComboBox(createModel()).also {
    it.isEditable = true
    it.editor = CheckComboBoxEditor()
    it.renderer = CheckComboBoxRenderer()
  }
  val title1 = "setEditable(true), setRenderer(...), setEditor(...)"
  box.add(createTitledPanel(title1, combo1))

  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel() = arrayOf(
  ComboItem(isEnabled = true, isEditable = true, text = "00000"),
  ComboItem(isEnabled = true, isEditable = false, text = "11111"),
  ComboItem(isEnabled = false, isEditable = true, text = "22222"),
  ComboItem(isEnabled = false, isEditable = false, text = "33333"),
)

private fun createTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

@Suppress("UseDataClass")
private class ComboItem(
  var isEnabled: Boolean = false,
  var isEditable: Boolean = false,
  var text: String? = "",
) {
  override fun toString() = "%s: %b, %b".format(text, isEnabled, isEditable)
}

private class CheckComboBoxRenderer : ListCellRenderer<ComboItem> {
  private val selectionBgc = Color(0x64_C8_FF)
  private val renderer = EditorPanel(ComboItem())

  override fun getListCellRendererComponent(
    list: JList<out ComboItem>,
    value: ComboItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    renderer.item = value
    // index == -1: the cell is painted in the combo box itself, not in the drop-down list
    val highlight = isSelected && index >= 0
    renderer.isOpaque = highlight
    renderer.background = if (highlight) selectionBgc else list.background
    return renderer
  }
}

private class CheckComboBoxEditor : ComboBoxEditor {
  private val editorPanel = EditorPanel(ComboItem())

  override fun selectAll() {
    editorPanel.selectAll()
  }

  override fun getItem() = editorPanel.item

  override fun setItem(anObject: Any?) {
    // The selected index is checked later because the editor might not
    // have been added to the JComboBox yet when this method is called
    EventQueue.invokeLater { updateEditingIndex() }
    editorPanel.item = anObject as? ComboItem ?: ComboItem()
  }

  private fun updateEditingIndex() {
    val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, editorComponent)
    // Keep the last valid index, since getSelectedIndex() returns -1
    // while the text is being edited
    val idx = (c as? JComboBox<*>)?.selectedIndex ?: -1
    if (idx >= 0) {
      editorPanel.editingIndex = idx
    }
  }

  override fun getEditorComponent() = editorPanel

  override fun addActionListener(l: ActionListener) {
    editorPanel.addActionListener(l)
  }

  override fun removeActionListener(l: ActionListener) {
    editorPanel.removeActionListener(l)
  }
}

private class EditorPanel(
  private val data: ComboItem,
) : JPanel() {
  private val enabledCheck = JCheckBox()
  private val editableCheck = JCheckBox()
  private val textField = JTextField("", 16)
  var editingIndex = -1

  var item: ComboItem
    get() {
      data.isEnabled = enabledCheck.isSelected
      data.isEditable = editableCheck.isSelected
      data.text = textField.text
      return data
    }
    set(item) {
      enabledCheck.isSelected = item.isEnabled

      editableCheck.isSelected = item.isEditable
      editableCheck.isEnabled = item.isEnabled

      textField.text = item.text
      textField.isEnabled = item.isEnabled
      textField.isEditable = item.isEditable
    }

  init {
    item = data

    enabledCheck.addActionListener {
      updateEditingItem { it.isEnabled = enabledCheck.isSelected }
    }
    enabledCheck.isOpaque = false
    enabledCheck.isFocusable = false

    editableCheck.addActionListener {
      updateEditingItem { it.isEditable = editableCheck.isSelected }
    }
    editableCheck.isOpaque = false
    editableCheck.isFocusable = false

    textField.addActionListener {
      updateEditingItem { it.text = textField.text }
    }
    textField.border = BorderFactory.createEmptyBorder()
    textField.isOpaque = false

    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
    add(enabledCheck)
    add(editableCheck)
    add(textField)
  }

  override fun isOpaque() = false

  // Applies the change to the model item being edited and reselects it
  private fun updateEditingItem(updater: (ComboItem) -> Unit) {
    val combo = SwingUtilities.getAncestorOfClass(
      JComboBox::class.java,
      this,
    ) as? JComboBox<*>
    // getItemAt(...) returns null if the editing index is out of range
    (combo?.getItemAt(editingIndex) as? ComboItem)?.also {
      updater(it)
      item = it
      combo.setSelectedIndex(editingIndex)
    }
  }

  fun selectAll() {
    textField.requestFocusInWindow()
    textField.selectAll()
  }

  fun addActionListener(l: ActionListener) {
    textField.addActionListener(l)
    enabledCheck.addActionListener(l)
    editableCheck.addActionListener(l)
  }

  fun removeActionListener(l: ActionListener) {
    textField.removeActionListener(l)
    enabledCheck.removeActionListener(l)
    editableCheck.removeActionListener(l)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
