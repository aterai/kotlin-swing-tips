package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val combo0 = JComboBox<ComboItem>(makeModel())
    combo0.setRenderer(CheckComboBoxRenderer<ComboItem>())

    val combo1 = JComboBox<ComboItem>(makeModel())
    combo1.setEditable(true)
    combo1.setEditor(CheckComboBoxEditor())
    combo1.setRenderer(CheckComboBoxRenderer<ComboItem>())

    val box = Box.createVerticalBox()
    box.add(makeTitledPanel("setEditable(false), setRenderer(...)", combo0))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("setEditable(true), setRenderer(...), setEditor(...)", combo1))
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(box, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeModel() = arrayOf(
      ComboItem(true, true, "00000"),
      ComboItem(true, false, "11111"),
      ComboItem(false, true, "22222"),
      ComboItem(false, false, "33333"))

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(c)
  }
}

open class ComboItem(var isEnabled: Boolean = false, var isEditable: Boolean = false, var text: String? = "")

class CheckComboBoxRenderer<E : ComboItem> : ListCellRenderer<E> {
  private val sbgc = Color(100, 200, 255)
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
      renderer.setOpaque(true)
      renderer.setBackground(sbgc)
    } else {
      renderer.setOpaque(false)
      renderer.setBackground(Color.WHITE)
    }
    return renderer
  }
}

class CheckComboBoxEditor : ComboBoxEditor {
  private val editor = EditorPanel(ComboItem())

  override fun selectAll() {
    editor.selectAll()
  }

  override fun getItem() = editor.item

  override fun setItem(anObject: Any) {
    EventQueue.invokeLater {
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, getEditorComponent())
      (c as? JComboBox<*>)?.also {
        val idx = it.getSelectedIndex()
        if (idx >= 0 && idx != editor.editingIndex) {
          println("setItem: $idx")
          editor.editingIndex = idx
        }
      }
    }
    // if (anObject is ComboItem) {
    //   editor.item = anObject
    // } else {
    //   editor.item = ComboItem()
    // }
    editor.item = anObject as? ComboItem ?: ComboItem()
  }

  override fun getEditorComponent() = editor

  override fun addActionListener(l: ActionListener) {
    println("addActionListener: " + l.javaClass.getName())
    editor.addActionListener(l)
  }

  override fun removeActionListener(l: ActionListener) {
    println("removeActionListener: " + l.javaClass.getName())
    editor.removeActionListener(l)
  }
}

class EditorPanel(private val data: ComboItem) : JPanel() {
  private val enabledCheck = JCheckBox()
  private val editableCheck = JCheckBox()
  private val textField = JTextField("", 16)
  var editingIndex = -1
  // data.index(this.data.index);
  var item: ComboItem
    get() {
      data.isEnabled = enabledCheck.isSelected()
      data.isEditable = editableCheck.isSelected()
      data.text = textField.getText()
      return data
    }
    set(item) {
      enabledCheck.setSelected(item.isEnabled)

      editableCheck.setSelected(item.isEditable)
      editableCheck.setEnabled(item.isEnabled)

      textField.setText(item.text)
      textField.setEnabled(item.isEnabled)
      textField.setEditable(item.isEditable)
    }

  init {
    item = data

    enabledCheck.addActionListener { e ->
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, this)
      if (c is JComboBox<*>) {
        val item = c.getItemAt(editingIndex) as ComboItem
        item.isEnabled = (e.getSource() as JCheckBox).isSelected()
        editableCheck.setEnabled(item.isEnabled)
        textField.setEnabled(item.isEnabled)
        c.setSelectedIndex(editingIndex)
      }
    }
    enabledCheck.setOpaque(false)
    enabledCheck.setFocusable(false)

    editableCheck.addActionListener { e ->
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, this)
      if (c is JComboBox<*>) {
        val item = c.getItemAt(editingIndex) as ComboItem
        item.isEditable = (e.getSource() as JCheckBox).isSelected()
        textField.setEditable(item.isEditable)
        c.setSelectedIndex(editingIndex)
      }
    }
    editableCheck.setOpaque(false)
    editableCheck.setFocusable(false)

    textField.addActionListener { e ->
      val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, this)
      if (c is JComboBox<*>) {
        val item = c.getItemAt(editingIndex) as ComboItem
        item.text = (e.getSource() as JTextField).getText()
        c.setSelectedIndex(editingIndex)
      }
    }
    textField.setBorder(BorderFactory.createEmptyBorder())
    textField.setOpaque(false)

    setOpaque(false)
    setLayout(BoxLayout(this, BoxLayout.LINE_AXIS))

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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
