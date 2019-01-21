package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.util.Objects
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout()) {
  init {
    val model = arrayOf("123456", "7890")
    val combo = JComboBox<String>(model)
    combo.setEditable(true)

    // val comboBox = JComboBox<String>(model)
    val comboBox = object : JComboBox<String>(model) {
      override fun updateUI() {
        getActionMap().put(ENTER_PRESSED, null)
        super.updateUI()
        val cb = this
        val defaultEnterPressedAction = getActionMap().get(ENTER_PRESSED)
        getActionMap().put(ENTER_PRESSED, object : AbstractAction() {
          override fun actionPerformed(e: ActionEvent) {
            val isPopupVisible = isPopupVisible()
            setPopupVisible(false)
            val m = getModel() as DefaultComboBoxModel<String>
            val str = Objects.toString(getEditor().getItem(), "")
            if (m.getIndexOf(str) < 0 && getInputVerifier()?.verify(cb) ?: false) {
              m.removeElement(str)
              m.insertElementAt(str, 0)
              if (m.getSize() > MAX_HISTORY) {
                m.removeElementAt(MAX_HISTORY)
              }
              setSelectedIndex(0)
              setPopupVisible(isPopupVisible)
            } else {
              defaultEnterPressedAction?.actionPerformed(e)
            }
          }
        })
      }
    }
    comboBox.setEditable(true)
    comboBox.setInputVerifier(LengthInputVerifier())
    comboBox.setEditor(object : BasicComboBoxEditor() {
      // private var editorComponent: Component? = null
      override fun getEditorComponent(): Component? {
        var tc = super.getEditorComponent()
        return if (tc is JTextComponent) JLayer<JTextComponent>(tc, ValidationLayerUI<JTextComponent>()) else tc
      }
    })
    comboBox.addPopupMenuListener(SelectItemMenuListener())

    val p = JPanel(GridLayout(5, 1)).apply {
      add(JLabel("Default:", SwingConstants.LEFT))
      add(combo)
      add(Box.createVerticalStrut(15))
      add(JLabel("6 >= str.length()", SwingConstants.LEFT))
      add(comboBox)
    }
    add(p, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private val MAX_HISTORY = 10
    private val ENTER_PRESSED = "enterPressed"
  }
}

internal class SelectItemMenuListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val c = e.getSource() as JComboBox<*>
    c.setSelectedItem(c.getEditor().getItem())
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) { /* not needed */
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) { /* not needed */
  }
}

// @see https://docs.oracle.com/javase/tutorial/uiswing/examples/misc/FieldValidatorProject/src/FieldValidator.java
internal class ValidationLayerUI<V : JTextComponent> : LayerUI<V>() {
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val cb = SwingUtilities.getAncestorOfClass(JComboBox::class.java, c)
    if (cb is JComboBox<*>) {
      cb.getInputVerifier()?.takeUnless { it.verify(cb) }?.let {
        val w = c.getWidth()
        val h = c.getHeight()
        val s = 8
        val pad = 5
        val x = w - pad - s
        val y = (h - s) / 2
        val g2 = g.create() as Graphics2D
        g2.translate(x, y)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setPaint(Color.RED)
        g2.fillRect(0, 0, s + 1, s + 1)
        g2.setPaint(Color.WHITE)
        g2.drawLine(0, 0, s, s)
        g2.drawLine(0, s, s, 0)
        g2.dispose()
      }
    }
  }
}

internal class LengthInputVerifier : InputVerifier() {
  override fun verify(c: JComponent): Boolean {
    if (c is JComboBox<*>) {
      val str = Objects.toString(c.getEditor().getItem(), "")
      return MAX_LEN - str.length >= 0
    }
    return false
  }

  companion object {
    private val MAX_LEN = 6
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
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
  })
}
