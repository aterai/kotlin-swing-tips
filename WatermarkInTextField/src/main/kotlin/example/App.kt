package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import javax.imageio.ImageIO
import javax.swing.*
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

private class WatermarkTextField : JTextField() {
  private val icon: Icon
  private var showWatermark = true
  private var listener: FocusListener? = null

  init {
    val cl = Thread.currentThread().contextClassLoader
    val url = cl.getResource("example/watermark.png")
    icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
      ?: UIManager.getIcon("html.missingImage")
  }

  override fun updateUI() {
    removeFocusListener(listener)
    super.updateUI()
    listener = object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        showWatermark = false
        e.component.repaint()
      }

      override fun focusLost(e: FocusEvent) {
        showWatermark = text.isEmpty()
        e.component.repaint()
      }
    }
    addFocusListener(listener)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (showWatermark) {
      val g2 = g.create() as? Graphics2D ?: return
      val i = insets
      val yy = (height - icon.iconHeight) / 2
      icon.paintIcon(this, g2, i.left, yy)
      g2.dispose()
    }
  }
}

private class GhostFocusListener(tf: JTextComponent) : FocusListener {
  private val ghostMessage = tf.text

  init {
    tf.foreground = INACTIVE_COLOR
  }

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
