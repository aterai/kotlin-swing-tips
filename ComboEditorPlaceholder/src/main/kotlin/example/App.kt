package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val combo1 = JComboBox(arrayOf("colors", "sports", "food"))
  // combo1.setEditable(true)
  combo1.selectedIndex = -1

  val arrays = arrayOf(
    arrayOf("blue", "violet", "red", "yellow"),
    arrayOf("basketball", "soccer", "football", "hockey"),
    arrayOf("hot dogs", "pizza", "ravioli", "bananas")
  )
  val combo2 = JComboBox<String>()
  combo2.isEditable = true

  combo1.addItemListener { e ->
    val combo = e.itemSelectable
    if (e.stateChange == ItemEvent.SELECTED && combo is JComboBox<*>) {
      val idx = combo.selectedIndex
      combo2.model = DefaultComboBoxModel(arrays[idx])
      combo2.selectedIndex = -1
    }
  }

  combo2.editor = object : BasicComboBoxEditor() {
    private var editorComponent: Component? = null

    override fun getEditorComponent(): Component? {
      (super.getEditorComponent() as? JTextComponent)?.also {
        editorComponent = editorComponent ?: JLayer(it, PlaceholderLayerUI("- Select type -"))
      }
      return editorComponent
    }
  }
  combo2.border = BorderFactory.createCompoundBorder(
    combo2.border,
    BorderFactory.createEmptyBorder(0, 2, 0, 0)
  )

  val p = JPanel(GridLayout(4, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
  p.add(JLabel("Category"))
  p.add(combo1)
  p.add(JLabel("Type"))
  p.add(combo2)

  val button = JButton("clear")
  button.addActionListener {
    combo1.selectedIndex = -1
    combo2.model = DefaultComboBoxModel()
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class PlaceholderLayerUI<E : JTextComponent>(hintMessage: String) : LayerUI<E>() {
  private val hint = JLabel(hintMessage)

  init {
    hint.foreground = INACTIVE
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    (c as? JLayer<*>)?.also {
      val tc = it.view as? JTextComponent ?: return
      if (tc.text.isEmpty() && !tc.hasFocus()) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = INACTIVE
        // println("getInsets: ${tc.getInsets()}")
        // println("getMargin: ${tc.getMargin()}")
        val i = tc.margin
        val d = hint.preferredSize
        SwingUtilities.paintComponent(g2, hint, tc, i.left, i.top, d.width, d.height)
        g2.dispose()
      }
    }
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.FOCUS_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent?) {
    super.uninstallUI(c)
    (c as? JLayer<*>)?.layerEventMask = 0
  }

  override fun processFocusEvent(e: FocusEvent, l: JLayer<out E>) {
    l.view.repaint()
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
