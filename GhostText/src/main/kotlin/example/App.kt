package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field1 = JTextField("Please enter your E-mail address")
  field1.addFocusListener(PlaceholderFocusListener(field1))

  val field2 = JTextField("History Search")
  field2.addFocusListener(PlaceholderFocusListener(field2))

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(makeTitledPanel("E-mail", field1))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("Search", field2))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("JLayer", JLayer(JTextField(), PlaceholderLayerUI<JTextComponent>("JLayer version"))))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private class PlaceholderFocusListener(tf: JTextComponent) : FocusListener {
  private val hintMessage = tf.text
  override fun focusGained(e: FocusEvent) {
    val tf = e.component as JTextComponent
    if (hintMessage == tf.text && INACTIVE == tf.foreground) {
      tf.foreground = UIManager.getColor("TextField.foreground")
      tf.text = ""
    }
  }

  override fun focusLost(e: FocusEvent) {
    (e.component as? JTextComponent)
      ?.takeIf { it.text.trim().isEmpty() }
      ?.also {
        it.foreground = INACTIVE
        it.text = hintMessage
      }
  }

  companion object {
    private val INACTIVE = UIManager.getColor("TextField.inactiveForeground")
  }

  init {
    tf.foreground = INACTIVE
  }
}

private class PlaceholderLayerUI<V : JTextComponent>(hintMessage: String) : LayerUI<V>() {
  private val hint: JLabel = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      foreground = UIManager.getColor("TextField.inactiveForeground")
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    ((c as? JLayer<*>)?.view as? JTextComponent)
      ?.takeIf { it.text.isEmpty() && !it.hasFocus() }
      ?.also {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = hint.background
        val i = it.insets
        val d = hint.preferredSize
        SwingUtilities.paintComponent(g2, hint, it, i.left, i.top, d.width, d.height)
        g2.dispose()
      }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.FOCUS_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    super.uninstallUI(c)
    (c as? JLayer<*>)?.layerEventMask = 0
  }

  override fun processFocusEvent(e: FocusEvent, l: JLayer<out V>) {
    l.view.repaint()
  }

  init {
    hint.text = hintMessage
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
