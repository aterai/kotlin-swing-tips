package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field1 = JTextField().also {
    val hint = "Please enter your E-mail address"
    val listener = PlaceholderFocusListener(hint)
    it.addFocusListener(listener)
    listener.update(it)
  }

  val field2 = object : JTextField() {
    private var listener: PlaceholderFocusListener? = null

    override fun updateUI() {
      removeFocusListener(listener)
      super.updateUI()
      val hint = "History Search"
      listener = PlaceholderFocusListener(hint)
      addFocusListener(listener)
      EventQueue.invokeLater { listener!!.update(this) }
    }
  }

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("E-mail", field1))
    it.add(Box.createVerticalStrut(10))
    it.add(makeTitledPanel("Search", field2))
    it.add(Box.createVerticalStrut(10))
    val ui = PlaceholderLayerUI<JTextComponent>("JLayer version")
    it.add(makeTitledPanel("JLayer", JLayer(JTextField(), ui)))
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private class PlaceholderFocusListener(private val hintMessage: String) : FocusListener {
  override fun focusGained(e: FocusEvent) {
    update(e.component)
  }

  override fun focusLost(e: FocusEvent) {
    update(e.component)
  }

  fun update(c: Component?) {
    (c as? JTextComponent)?.also {
      val txt = it.text.trim()
      if (txt.isEmpty()) {
        it.foreground = UIManager.getColor("TextField.inactiveForeground")
        it.text = hintMessage
      } else {
        it.foreground = UIManager.getColor("TextField.foreground")
        if (txt == hintMessage) {
          it.text = ""
        }
      }
    }
  }
}

private class PlaceholderLayerUI<V : JTextComponent>(hintMessage: String) : LayerUI<V>() {
  private val hint = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      foreground = UIManager.getColor("TextField.inactiveForeground")
    }
  }

  init {
    hint.text = hintMessage
  }

  override fun updateUI(l: JLayer<out V?>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(hint)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    ((c as? JLayer<*>)?.view as? JTextComponent)
      ?.takeIf { it.text.isEmpty() && !it.hasFocus() }
      ?.also {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = hint.background
        val r = SwingUtilities.calculateInnerArea(it, null)
        val d = hint.preferredSize
        val yy = (r.centerY - d.height / 2.0).toInt()
        SwingUtilities.paintComponent(g2, hint, it, r.x, yy, d.width, d.height)
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
