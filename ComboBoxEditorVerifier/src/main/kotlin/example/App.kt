package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.JTextComponent

private const val MAX_HISTORY = 10
private const val ENTER_PRESSED = "enterPressed"

fun makeUI(): Component {
  val model = arrayOf("123456", "7890")
  val combo = JComboBox(model)
  combo.isEditable = true

  val comboBox = object : JComboBox<String>(model) {
    override fun updateUI() {
      actionMap.put(ENTER_PRESSED, null)
      super.updateUI()
      val cb = this
      val defaultAction = actionMap[ENTER_PRESSED]
      val action = object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          val isPopupVisible = isPopupVisible
          setPopupVisible(false)
          val m = getModel() as? DefaultComboBoxModel<String> ?: return
          val str = getEditor().item?.toString() ?: ""
          if (m.getIndexOf(str) < 0 && inputVerifier?.verify(cb) == true) {
            m.removeElement(str)
            m.insertElementAt(str, 0)
            if (m.size > MAX_HISTORY) {
              m.removeElementAt(MAX_HISTORY)
            }
            selectedIndex = 0
            setPopupVisible(isPopupVisible)
          } else {
            defaultAction?.actionPerformed(e)
          }
        }
      }
      actionMap.put(ENTER_PRESSED, action)
    }
  }
  comboBox.isEditable = true
  comboBox.inputVerifier = LengthInputVerifier()
  comboBox.editor = object : BasicComboBoxEditor() {
    private var validationEditor: Component? = null

    override fun getEditorComponent(): Component? {
      (super.getEditorComponent() as? JTextComponent)?.also {
        validationEditor = validationEditor ?: JLayer(it, ValidationLayerUI())
      }
      return validationEditor
    }
  }
  comboBox.addPopupMenuListener(SelectItemMenuListener())

  val p = JPanel(GridLayout(5, 1)).also {
    it.add(JLabel("Default:", SwingConstants.LEFT))
    it.add(combo)
    it.add(Box.createVerticalStrut(15))
    it.add(JLabel("6 >= str.length()", SwingConstants.LEFT))
    it.add(comboBox)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SelectItemMenuListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val c = e.source as? JComboBox<*> ?: return
    c.selectedItem = c.editor.item
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }
}

// @see https://docs.oracle.com/javase/tutorial/uiswing/examples/misc/FieldValidatorProject/src/FieldValidator.java
private class ValidationLayerUI<V : JTextComponent> : LayerUI<V>() {
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val cb = SwingUtilities.getAncestorOfClass(JComboBox::class.java, c)
    if (cb is JComboBox<*>) {
      cb.inputVerifier?.takeUnless { it.verify(cb) }?.also {
        val w = c.width
        val h = c.height
        val s = 8
        val pad = 5
        val x = w - pad - s
        val y = (h - s) / 2
        val g2 = g.create() as? Graphics2D ?: return
        g2.translate(x, y)
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.paint = Color.RED
        g2.fillRect(0, 0, s + 1, s + 1)
        g2.paint = Color.WHITE
        g2.drawLine(0, 0, s, s)
        g2.drawLine(0, s, s, 0)
        g2.dispose()
      }
    }
  }
}

private class LengthInputVerifier : InputVerifier() {
  override fun verify(c: JComponent) = (c as? JComboBox<*>)?.let {
    val iv = it.editor.item
      ?.toString()
      ?.length ?: 0
    MAX_LEN - iv >= 0
  } ?: false

  companion object {
    private const val MAX_LEN = 6
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
