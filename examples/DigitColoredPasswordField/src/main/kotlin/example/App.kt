package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Document
import javax.swing.text.DocumentFilter
import javax.swing.text.Element
import javax.swing.text.IconView
import javax.swing.text.JTextComponent
import javax.swing.text.LabelView
import javax.swing.text.ParagraphView
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory

val FONT = Font(Font.MONOSPACED, Font.PLAIN, 12)

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1, 5, 25))
  val p1 = makePasswordPanel1()
  p.add(makeTitledPanel("JPasswordField#setEchoChar(...) + HighlightFilter", p1))
  val p2 = makePasswordPanel2()
  p.add(makeTitledPanel("CardLayout + (JPasswordField <> JTextPane)", p2))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(25, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePasswordPanel1(): JPanel {
  val password = DigitHighlightPasswordField(40)
  password.setFont(FONT)
  password.setAlignmentX(Component.RIGHT_ALIGNMENT)
  password.text = "!1l2c$%34e5&6#7=8g9O0"

  val button = JToggleButton()
  button.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    password.echoChar = if (b) '\u0000' else getUIEchoChar()
  }
  initEyeButton(button)
  val p = OverlayLayoutPanel()
  p.add(button)
  p.add(password)
  return p
}

private fun makePasswordPanel2(): JPanel {
  val password = JPasswordField(40)
  password.setFont(FONT)
  password.text = "!1l2c$%34e5&6#7=8g9O0"
  val revealPassword = makeRevealPassword(password)
  val cardLayout = CardLayout()
  val p = object : JPanel(cardLayout) {
    override fun updateUI() {
      super.updateUI()
      setAlignmentX(RIGHT_ALIGNMENT)
    }
  }
  p.add(password, PasswordField.HIDE.toString())
  p.add(revealPassword, PasswordField.SHOW.toString())

  val button = JToggleButton()
  button.addActionListener { e ->
    val b = (e.source as? AbstractButton)?.isSelected == true
    if (b) {
      copyText(password.document, revealPassword.styledDocument)
      cardLayout.show(p, PasswordField.SHOW.toString())
    } else {
      copyText(revealPassword.styledDocument, password.document)
      cardLayout.show(p, PasswordField.HIDE.toString())
    }
  }
  initEyeButton(button)

  val panel = OverlayLayoutPanel()
  panel.add(button)
  panel.add(p)
  return panel
}

private fun copyText(src: Document, dst: Document) {
  runCatching {
    dst.remove(0, dst.length)
    val text = src.getText(0, src.length)
    dst.insertString(0, text, null)
  }.onFailure {
    it.printStackTrace()
  }
}

private fun getUIEchoChar() = UIManager.get("PasswordField.echoChar") as? Char ?: '*'

private fun initEyeButton(b: AbstractButton) {
  b.isFocusable = false
  b.isOpaque = false
  b.isContentAreaFilled = false
  b.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
  b.alignmentX = Component.RIGHT_ALIGNMENT
  b.alignmentY = Component.CENTER_ALIGNMENT
  b.icon = EyeIcon(Color.BLUE)
  b.rolloverIcon = EyeIcon(Color.DARK_GRAY)
  b.selectedIcon = EyeIcon(Color.BLUE)
  b.rolloverSelectedIcon = EyeIcon(Color.BLUE)
  b.toolTipText = "show/hide passwords"
}

private fun makeRevealPassword(password: JPasswordField): JTextPane {
  val textPane = OneLineTextPane()
  textPane.setBorder(password.border)
  textPane.setFont(password.getFont())
  val doc = textPane.styledDocument
  if (doc is AbstractDocument) {
    doc.documentFilter = HighlightDocumentFilter()
    runCatching {
      val length = password.document.length
      val text = password.document.getText(0, length)
      doc.insertString(0, text, SimpleAttributeSet())
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(textPane)
    }
  }
  return textPane
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.setBorder(BorderFactory.createTitledBorder(title))
  p.add(c)
  return p
}

private enum class PasswordField { SHOW, HIDE }

private class DigitHighlightPasswordField(
  columns: Int,
) : JPasswordField(columns) {
  override fun setEchoChar(c: Char) {
    super.setEchoChar(c)
    val doc = document
    if (doc is AbstractDocument) {
      val reveal = c == '\u0000'
      if (reveal) {
        doc.documentFilter = HighlightFilter(this)
        runCatching {
          doc.remove(0, 0)
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(this)
        }
      } else {
        highlighter.removeAllHighlights()
        doc.documentFilter = null
      }
    }
  }
}

private class HighlightFilter(
  private val field: JTextComponent,
) : DocumentFilter() {
  private val painter = DefaultHighlightPainter(Color.YELLOW)
  private val pattern = Pattern.compile("\\d")

  @Throws(BadLocationException::class)
  override fun insertString(
    fb: FilterBypass,
    offset: Int,
    text: String?,
    attr: AttributeSet?,
  ) {
    super.insertString(fb, offset, text, attr)
    update(fb)
  }

  @Throws(BadLocationException::class)
  override fun remove(fb: FilterBypass, offset: Int, length: Int) {
    super.remove(fb, offset, length)
    update(fb)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String?,
    attrs: AttributeSet?,
  ) {
    super.replace(fb, offset, length, text, attrs)
    update(fb)
  }

  private fun update(fb: FilterBypass) {
    val doc = fb.document
    field.highlighter.removeAllHighlights()
    runCatching {
      val highlighter = field.highlighter
      val text = doc.getText(0, doc.length)
      val matcher = pattern.matcher(text)
      var pos = 0
      while (matcher.find(pos) && !matcher.group().isEmpty()) {
        pos = matcher.end()
        highlighter.addHighlight(matcher.start(), pos, painter)
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(field)
    }
  }
}

private class HighlightDocumentFilter : DocumentFilter() {
  private val defAttr = SimpleAttributeSet()
  private val numAttr = SimpleAttributeSet()
  private val pattern = Pattern.compile("\\d")

  init {
    StyleConstants.setForeground(defAttr, Color.BLACK)
    StyleConstants.setForeground(numAttr, Color.RED)
  }

  @Throws(BadLocationException::class)
  override fun insertString(
    fb: FilterBypass,
    offset: Int,
    text: String?,
    attr: AttributeSet?,
  ) {
    super.insertString(fb, offset, text, attr)
    update(fb)
  }

  @Throws(BadLocationException::class)
  override fun remove(fb: FilterBypass, offset: Int, length: Int) {
    super.remove(fb, offset, length)
    update(fb)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String?,
    attrs: AttributeSet?,
  ) {
    super.replace(fb, offset, length, text, attrs)
    update(fb)
  }

  @Throws(BadLocationException::class)
  private fun update(fb: FilterBypass) {
    val doc = fb.document as? StyledDocument ?: return
    val text = doc.getText(0, doc.length)
    doc.setCharacterAttributes(0, doc.length, defAttr, true)
    val m = pattern.matcher(text)
    while (m.find()) {
      doc.setCharacterAttributes(m.start(), m.end() - m.start(), numAttr, true)
    }
  }
}

private class OverlayLayoutPanel : JPanel() {
  override fun updateUI() {
    super.updateUI()
    setLayout(OverlayLayout(this))
  }

  override fun isOptimizedDrawingEnabled() = false
}

private class EyeIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    g2.paint = color
    val iw = iconWidth
    val ih = iconHeight
    val s = iconWidth / 12.0
    g2.stroke = BasicStroke(s.toFloat())
    val w = iw - s * 2.0
    val h = ih - s * 2.0
    val r = w * 3.0 / 4.0 - s * 2.0
    val x0 = w / 2.0 - r + s
    val eye = Area(Ellipse2D.Double(x0, s * 4.0 - r, r * 2.0, r * 2.0))
    eye.intersect(Area(Ellipse2D.Double(x0, h - r - s * 2.0, r * 2.0, r * 2.0)))
    g2.draw(eye)
    val rr = iw / 6.0
    g2.draw(Ellipse2D.Double(iw / 2.0 - rr, ih / 2.0 - rr, rr * 2.0, rr * 2.0))
    if (c is AbstractButton) {
      val m = c.model
      if (m.isSelected || m.isPressed) {
        val l = Line2D.Double(iw / 6.0, ih * 5.0 / 6.0, iw * 5.0 / 6.0, ih / 6.0)
        val at = AffineTransform.getTranslateInstance(-s, 0.0)
        g2.paint = Color.WHITE
        g2.draw(at.createTransformedShape(l))
        g2.paint = color
        g2.draw(l)
      }
    }
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private class OneLineTextPane : JTextPane() {
  override fun updateUI() {
    super.updateUI()
    val key = "Do-Nothing"
    val im = getInputMap(WHEN_FOCUSED)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), key)
    actionMap.put(
      key,
      object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          // Do nothing
        }
      },
    )
    editorKit = NoWrapEditorKit()
    enableInputMethods(false)
  }

  override fun scrollRectToVisible(rect: Rectangle) {
    rect.grow(getInsets().right, 0)
    super.scrollRectToVisible(rect)
  }
}

private class NoWrapParagraphView(
  elem: Element?,
) : ParagraphView(elem) {
  override fun calculateMinorAxisRequirements(
    axis: Int,
    r: SizeRequirements?,
  ): SizeRequirements =
    super.calculateMinorAxisRequirements(axis, r).also {
      it.minimum = it.preferred
    }

  override fun getFlowSpan(index: Int) = Int.MAX_VALUE
}

private class NoWrapViewFactory : ViewFactory {
  override fun create(elem: Element) = when (elem.name) {
    AbstractDocument.ParagraphElementName -> NoWrapParagraphView(elem)
    AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
    StyleConstants.ComponentElementName -> ComponentView(elem)
    StyleConstants.IconElementName -> IconView(elem)
    else -> LabelView(elem)
  }
}

private class NoWrapEditorKit : StyledEditorKit() {
  override fun getViewFactory() = NoWrapViewFactory()
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
