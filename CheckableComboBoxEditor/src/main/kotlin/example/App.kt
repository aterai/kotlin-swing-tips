package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.io.Serializable
import java.util.Objects
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val combo0 = JComboBox(makeModel())
  combo0.renderer = CheckComboBoxRenderer<ComboItem>()

  val combo1 = JComboBox(makeModel())
  combo1.isEditable = true
  combo1.editor = CheckComboBoxEditor()
  combo1.renderer = CheckComboBoxRenderer<ComboItem>()

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("setEditable(false), setRenderer(...)", combo0))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("setEditable(true), setRenderer(...), setEditor(...)", combo1))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = arrayOf(
  ComboItem(isEnabled = true, isEditable = true, text = "00000"),
  ComboItem(isEnabled = true, isEditable = false, text = "11111"),
  ComboItem(isEnabled = false, isEditable = true, text = "22222"),
  ComboItem(isEnabled = false, isEditable = false, text = "33333")
)

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private open class ComboItem(
  var isEnabled: Boolean = false,
  var isEditable: Boolean = false,
  var text: String? = ""
) : Serializable {
  override fun hashCode(): Int {
    return Objects.hashCode(this)
  }

  override fun equals(other: Any?): Boolean {
    return other is ComboItem && other.text == text
  }

  override fun toString(): String {
    return "%s: %b, %b".format(text, isEnabled, isEditable)
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}

private class CheckComboBoxRenderer<E : ComboItem> : ListCellRenderer<E> {
  private val bgc = Color(100, 200, 255)
  private val renderer = EditorPanel(ComboItem())

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    renderer.item = value
    if (isSelected && index >= 0) {
      renderer.isOpaque = true
      renderer.background = bgc
    } else {
      renderer.isOpaque = false
      renderer.background = Color.WHITE
    }
    return renderer
  }
}

private class CheckComboBoxEditor : ComboBoxEditor {
  private val editor = EditorPanel(ComboItem())

  override fun selectAll() {
    editor.selectAll()
  }

  override fun getItem() = editor.item

  override fun setItem(anObject: Any) {
    EventQueue.invokeLater {
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, editorComponent)
      (c as? JComboBox<*>)?.also {
        val idx = it.selectedIndex
        if (idx >= 0 && idx != editor.editingIndex) {
          // println("setItem: $idx")
          editor.editingIndex = idx
        }
      }
    }
    editor.item = anObject as? ComboItem ?: ComboItem()
  }

  override fun getEditorComponent() = editor

  override fun addActionListener(l: ActionListener) {
    // println("addActionListener: ${l.javaClass.name}")
    editor.addActionListener(l)
  }

  override fun removeActionListener(l: ActionListener) {
    // println("removeActionListener: ${l.javaClass.name}")
    editor.removeActionListener(l)
  }
}

private class EditorPanel(private val data: ComboItem) : JPanel() {
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

    enabledCheck.addActionListener { e ->
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, this)
      val check = c as? JComboBox<*>
      (check?.getItemAt(editingIndex) as? ComboItem)?.also {
        it.isEnabled = (e.source as? JCheckBox)?.isSelected ?: false
        editableCheck.isEnabled = it.isEnabled
        textField.isEnabled = it.isEnabled
        check.setSelectedIndex(editingIndex)
      }
    }
    enabledCheck.isOpaque = false
    enabledCheck.isFocusable = false

    editableCheck.addActionListener { e ->
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, this)
      val check = c as? JComboBox<*>
      (check?.getItemAt(editingIndex) as? ComboItem)?.also {
        it.isEditable = (e.source as? JCheckBox)?.isSelected ?: false
        textField.isEditable = it.isEditable
        check.setSelectedIndex(editingIndex)
      }
    }
    editableCheck.isOpaque = false
    editableCheck.isFocusable = false

    textField.addActionListener { e ->
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, this)
      val check = c as? JComboBox<*>
      (check?.getItemAt(editingIndex) as? ComboItem)?.also {
        it.text = (e.source as? JTextField)?.text ?: ""
        check.setSelectedIndex(editingIndex)
      }
    }
    textField.border = BorderFactory.createEmptyBorder()
    textField.isOpaque = false

    isOpaque = false
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)

    add(enabledCheck)
    add(editableCheck)
    add(textField)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
