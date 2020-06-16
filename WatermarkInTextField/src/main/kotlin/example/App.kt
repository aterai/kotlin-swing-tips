package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field1 = JTextField("Please enter your E-mail address")
  field1.addFocusListener(GhostFocusListener(field1))

  val field2 = WatermarkTextField()
  val amKey = "clearGlobalFocus"
  val action = object : AbstractAction(amKey) {
    override fun actionPerformed(e: ActionEvent) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner()
    }
  }
  field2.actionMap.put(amKey, action)
  val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
  val im = field2.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, modifiers), amKey)

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(makeTitledPanel("E-mail", field1))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Search", field2))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class WatermarkTextField : JTextField(), FocusListener {
  private val image = ImageIcon(javaClass.getResource("watermark.png"))
  private var showWatermark = true
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (showWatermark) {
      val g2 = g.create() as? Graphics2D ?: return
      val i = insets
      val yy = (height - image.iconHeight) / 2
      g2.drawImage(image.image, i.left, yy, this)
      g2.dispose()
    }
  }

  override fun focusGained(e: FocusEvent) {
    showWatermark = false
    repaint()
  }

  override fun focusLost(e: FocusEvent) {
    showWatermark = text.trim().isEmpty()
    repaint()
  }

  init {
    addFocusListener(this)
  }
}

private class GhostFocusListener(tf: JTextComponent) : FocusListener {
  private val ghostMessage = tf.text
  override fun focusGained(e: FocusEvent) {
    (e.component as? JTextComponent)?.also {
      if (ghostMessage == it.text && INACTIVE_COLOR == it.foreground) {
        it.foreground = ORIGINAL_COLOR
        it.text = ""
      }
    }
  }

  override fun focusLost(e: FocusEvent) {
    (e.component as? JTextComponent)?.also {
      if (it.text.trim().isEmpty()) {
        it.foreground = INACTIVE_COLOR
        it.text = ghostMessage
      }
    }
  }

  companion object {
    private val INACTIVE_COLOR = UIManager.getColor("TextField.inactiveForeground")
    private val ORIGINAL_COLOR = UIManager.getColor("TextField.foreground")
  }

  init {
    tf.foreground = INACTIVE_COLOR
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
