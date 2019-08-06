package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout()) {
  init {
    val combo1 = JComboBox<String>(arrayOf("colors", "sports", "food"))
    combo1.setEditable(true)
    combo1.setSelectedIndex(-1)

    val arrays = arrayOf(
        arrayOf("blue", "violet", "red", "yellow"),
        arrayOf("basketball", "soccer", "football", "hockey"),
        arrayOf("hot dogs", "pizza", "ravioli", "bananas"))
    val combo2 = JComboBox<String>()
    combo2.setEditable(true)

    combo1.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        val idx = (e.getItemSelectable() as JComboBox<*>).getSelectedIndex()
        combo2.setModel(DefaultComboBoxModel<String>(arrays[idx]))
        combo2.setSelectedIndex(-1)
      }
    }

    combo2.setEditor(object : BasicComboBoxEditor() {
      private var editorComponent: Component? = null

      override fun getEditorComponent(): Component? {
        (super.getEditorComponent() as? JTextComponent)?.also {
          editorComponent = editorComponent ?: JLayer(it, PlaceholderLayerUI("- Select type -"))
        }
        return editorComponent
      }
    })
    combo2.setBorder(
      BorderFactory.createCompoundBorder(
        combo2.getBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 0)
      )
    )

    val p = JPanel(GridLayout(4, 1, 5, 5))
    setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20))
    p.add(JLabel("Category"))
    p.add(combo1)
    p.add(JLabel("Type"))
    p.add(combo2)

    val button = JButton("clear")
    button.addActionListener {
      combo1.setSelectedIndex(-1)
      combo2.setModel(DefaultComboBoxModel<String>())
    }

    add(p, BorderLayout.NORTH)
    add(button, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class PlaceholderLayerUI<E : JTextComponent>(hintMessage: String) : LayerUI<E>() {
  private val hint: JLabel

  init {
    this.hint = JLabel(hintMessage)
    hint.setForeground(INACTIVE)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    (c as? JLayer<*>)?.also {
      val tc = it.getView() as? JTextComponent ?: return@paint
      if (tc.getText().length == 0 && !tc.hasFocus()) {
        val g2 = g.create() as Graphics2D
        g2.setPaint(INACTIVE)
        // println("getInsets: ${tc.getInsets()}")
        // println("getMargin: ${tc.getMargin()}")
        val i = tc.getMargin()
        val d = hint.getPreferredSize()
        SwingUtilities.paintComponent(g2, hint, tc, i.left, i.top, d.width, d.height)
        g2.dispose()
      }
    }
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.FOCUS_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent?) {
    super.uninstallUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(0)
  }

  override fun processFocusEvent(e: FocusEvent, l: JLayer<out E>) {
    l.getView().repaint()
  }

  companion object {
    private val INACTIVE = UIManager.getColor("TextField.inactiveForeground")
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
