package example

import java.awt.*
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicPasswordFieldUI
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret
import javax.swing.text.DocumentFilter
import javax.swing.text.Element
import javax.swing.text.JTextComponent
import javax.swing.text.PasswordView
import javax.swing.text.Segment
import javax.swing.text.Utilities

fun makeUI(): Component {
  val password = object : JPasswordField(6) {
    override fun updateUI() {
      super.updateUI()
      setUI(object : BasicPasswordFieldUI() {
        override fun create(elem: Element) = PasswordView2(elem)
      })
    }
  }

  val box = Box.createVerticalBox()
  box.add(makePasswordField(JPasswordField(6)))
  box.add(Box.createVerticalStrut(10))
  box.add(makePasswordField(password))

  return JPanel(GridBagLayout()).also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePasswordField(password: JPasswordField): Component {
  password.caret = object : DefaultCaret() {
    override fun isSelectionVisible() = false
  }
  password.selectionColor = Color.WHITE
  password.font = password.font.deriveFont(30f)
  val doc = password.document
  if (doc is AbstractDocument) {
    doc.documentFilter = PinCodeDocumentFilter()
  }
  doc.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      if (e.document.length >= PinCodeDocumentFilter.MAX) {
        JOptionPane.showMessageDialog(password.rootPane, "Try PIN code verification.")
        EventQueue.invokeLater { password.text = "" }
      }
    }

    override fun removeUpdate(e: DocumentEvent) {
      // Do nothing
    }

    override fun changedUpdate(e: DocumentEvent) {
      // Do nothing
    }
  })
  return JLayer(password, PlaceholderLayerUI<JTextComponent>("PIN"))
}

private class PinCodeDocumentFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String,
    attrs: AttributeSet?,
  ) {
    val str = fb.document.getText(0, fb.document.length) + text
    if (str.length <= MAX && str.matches("\\d+".toRegex())) {
      super.replace(fb, offset, length, text, attrs)
    }
  }

  companion object {
    const val MAX = 4
  }
}

private class PasswordView2(
  elem: Element,
) : PasswordView(elem) {
  @Throws(BadLocationException::class)
  override fun drawUnselectedText(
    g: Graphics,
    x: Int,
    y: Int,
    p0: Int,
    p1: Int,
  ) = drawText(g, x, y, p0, p1)

  @Throws(BadLocationException::class)
  private fun drawText(
    g: Graphics,
    x: Int,
    y: Int,
    p0: Int,
    p1: Int,
  ): Int {
    val c = container
    var j = x
    if (c is JPasswordField) {
      if (c.isEnabled) {
        g.color = c.foreground
      } else {
        g.color = c.disabledTextColor
      }
      val g2 = g as? Graphics2D ?: return 0
      val echoChar = c.echoChar
      val n = p1 - p0
      for (i in 0..<n) {
        j = if (i == n - 1) {
          drawLastChar(g2, j, y, i)
        } else {
          drawEchoCharacter(g, j, y, echoChar)
        }
      }
    }
    return j
  }

  @Throws(BadLocationException::class)
  private fun drawLastChar(
    g: Graphics,
    x: Int,
    y: Int,
    p1: Int,
  ): Int {
    val font = g.font
    val frc = g.fontMetrics.fontRenderContext
    val w = font.getStringBounds("0", frc).width
    val sz = ((font.size2D - w) / 2.0).toInt()
    val doc = document
    val s = Segment()
    doc.getText(p1, 1, s)
    return Utilities.drawTabbedText(s, x + sz, y, g, this, p1)
  }
}

private class PlaceholderLayerUI<E : JTextComponent>(
  hintMessage: String,
) : LayerUI<E>() {
  private val hint = object : JLabel(hintMessage) {
    override fun updateUI() {
      super.updateUI()
      val inactive = "TextField.inactiveForeground"
      foreground = UIManager.getLookAndFeelDefaults().getColor(inactive)
    }
  }

  override fun updateUI(l: JLayer<out E>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(hint)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    ((c as? JLayer<*>)?.view as? JTextComponent)?.also {
      if (it.text.isEmpty()) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = hint.foreground
        val r = SwingUtilities.calculateInnerArea(it, null)
        val d = hint.preferredSize
        val yy = (r.y + (r.height - d.height) / 2.0).toInt()
        SwingUtilities.paintComponent(g2, hint, it, r.x, yy, d.width, d.height)
        g2.dispose()
      }
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.FOCUS_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    super.uninstallUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = 0
    }
  }

  public override fun processFocusEvent(
    e: FocusEvent,
    l: JLayer<out E>,
  ) {
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
