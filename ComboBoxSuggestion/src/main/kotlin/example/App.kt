package example

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val array = arrayOf(
    "111",
    "1111222",
    "111122233",
    "111122233444",
    "1234",
    "12567",
    "2221",
    "22212",
  )
  val combo = JComboBox(array)
  combo.isEditable = true
  combo.selectedIndex = -1
  val field = combo.editor.editorComponent
  (field as? JTextField)?.text = ""
  field.addKeyListener(ComboKeyHandler(combo))

  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder("Auto-Completion ComboBox")
  p.add(combo, BorderLayout.NORTH)

  val box = Box.createVerticalBox()
  box.add(makeHelpPanel())
  box.add(Box.createVerticalStrut(5))
  box.add(p)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeHelpPanel(): Component {
  val lp = JPanel(GridLayout(2, 1, 2, 2))
  lp.add(JLabel("Char: show Popup"))
  lp.add(JLabel("ESC: hide Popup"))

  val rp = JPanel(GridLayout(2, 1, 2, 2))
  rp.add(JLabel("RIGHT: Completion"))
  rp.add(JLabel("ENTER: Add/Selection"))

  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("Help")

  val c = GridBagConstraints()
  c.insets = Insets(0, 5, 0, 5)
  c.fill = GridBagConstraints.BOTH
  c.weighty = 1.0

  c.weightx = 1.0
  p.add(lp, c)

  c.weightx = 0.0
  p.add(JSeparator(SwingConstants.VERTICAL), c)

  c.weightx = 1.0
  p.add(rp, c)

  return p
}

private class ComboKeyHandler(
  private val comboBox: JComboBox<String>,
) : KeyAdapter() {
  private val list = mutableListOf<String>()
  private var shouldHide = false

  init {
    for (i in 0..<comboBox.model.size) {
      list.add(comboBox.getItemAt(i))
    }
  }

  override fun keyTyped(e: KeyEvent) {
    EventQueue.invokeLater {
      val text = (e.component as? JTextField)?.text ?: ""
      if (text.isEmpty()) {
        val m = DefaultComboBoxModel(list.toTypedArray())
        setSuggestionModel(comboBox, m, "")
        comboBox.hidePopup()
      } else {
        val m = getSuggestedModel(list, text)
        if (m.size == 0 || shouldHide) {
          comboBox.hidePopup()
        } else {
          setSuggestionModel(comboBox, m, text)
          comboBox.showPopup()
        }
      }
    }
  }

  override fun keyPressed(e: KeyEvent) {
    val textField = e.component as? JTextField ?: return
    val text = textField.text
    shouldHide = false
    when (e.keyCode) {
      KeyEvent.VK_RIGHT -> list.first { it.contains(text) }.also {
        textField.text = it
      }

      KeyEvent.VK_ENTER -> enter(text)

      KeyEvent.VK_ESCAPE -> shouldHide = true
    }
  }

  private fun enter(text: String) {
    if (!list.contains(text)) {
      list.add(text)
      list.sort()
      setSuggestionModel(comboBox, getSuggestedModel(list, text), text)
    }
    shouldHide = true
  }

  private fun setSuggestionModel(
    cb: JComboBox<String>,
    m: ComboBoxModel<String>,
    txt: String,
  ) {
    cb.model = m
    cb.selectedIndex = -1
    (cb.editor.editorComponent as? JTextField)?.text = txt
  }

  private fun getSuggestedModel(
    list: List<String>,
    text: String,
  ): ComboBoxModel<String> {
    val m = DefaultComboBoxModel<String>()
    for (s in list) {
      if (s.startsWith(text)) {
        m.addElement(s)
      }
    }
    return m
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
