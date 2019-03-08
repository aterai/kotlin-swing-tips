package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val array = arrayOf(
        "aaaa", "aaaabbb", "aaaabbbcc", "aaaabbbccddd", "abcde", "abefg", "bbb1", "bbb12")
    val combo = JComboBox<String>(array)
    combo.setEditable(true)
    combo.setSelectedIndex(-1)
    val field = combo.getEditor().getEditorComponent() as JTextField
    field.setText("")
    field.addKeyListener(ComboKeyHandler(combo))

    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder("Auto-Completion ComboBox"))
    p.add(combo, BorderLayout.NORTH)

    val box = Box.createVerticalBox()
    box.add(makeHelpPanel())
    box.add(Box.createVerticalStrut(5))
    box.add(p)
    add(box, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeHelpPanel(): Component {
    val lp = JPanel(GridLayout(2, 1, 2, 2))
    lp.add(JLabel("Char: show Popup"))
    lp.add(JLabel("ESC: hide Popup"))

    val rp = JPanel(GridLayout(2, 1, 2, 2))
    rp.add(JLabel("RIGHT: Completion"))
    rp.add(JLabel("ENTER: Add/Selection"))

    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder("Help"))

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
}

internal class ComboKeyHandler(private val comboBox: JComboBox<String>) : KeyAdapter() {
  private val list = mutableListOf<String>()
  private var shouldHide = false

  init {
    for (i in 0 until comboBox.getModel().getSize()) {
      list.add(comboBox.getItemAt(i))
    }
  }

  override fun keyTyped(e: KeyEvent) {
    EventQueue.invokeLater {
      val text = (e.getComponent() as? JTextField)?.getText() ?: return@invokeLater
      val m: ComboBoxModel<String>
      if (text.isEmpty()) {
        m = DefaultComboBoxModel<String>(list.toTypedArray())
        setSuggestionModel(comboBox, m, "")
        comboBox.hidePopup()
      } else {
        m = getSuggestedModel(list, text)
        if (m.getSize() == 0 || shouldHide) {
          comboBox.hidePopup()
        } else {
          setSuggestionModel(comboBox, m, text)
          comboBox.showPopup()
        }
      }
    }
  }

  override fun keyPressed(e: KeyEvent) {
    val textField = e.getComponent() as? JTextField ?: return
    val text = textField.getText()
    shouldHide = false
    when (e.getKeyCode()) {
      KeyEvent.VK_RIGHT -> for (s in list) {
        if (s.startsWith(text)) {
          textField.setText(s)
          return
        }
      }
      KeyEvent.VK_ENTER -> {
        if (!list.contains(text)) {
          list.add(text)
          list.sort()
          setSuggestionModel(comboBox, getSuggestedModel(list, text), text)
        }
        shouldHide = true
      }
      KeyEvent.VK_ESCAPE -> shouldHide = true
      else -> {
      }
    }
  }

  private fun setSuggestionModel(comboBox: JComboBox<String>, mdl: ComboBoxModel<String>, str: String) {
    comboBox.setModel(mdl)
    comboBox.setSelectedIndex(-1)
    (comboBox.getEditor().getEditorComponent() as JTextField).setText(str)
  }

  private fun getSuggestedModel(list: List<String>, text: String): ComboBoxModel<String> {
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
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
