package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.Position
import javax.swing.text.View

val FONT = Font(Font.MONOSPACED, Font.PLAIN, 12)

private fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1, 5, 25))
  val p1 = makeTextField()
  p.add(makeTitledPanel("JTextField + HighlightFilter", p1))
  val p2 = makePasswordPanel()
  p.add(makeTitledPanel("JPasswordField + HighlightFilter", p2))
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(25, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTextField(): JPanel {
  val txt = "The quick brown fox jumps over the lazy dog."
  val field = object : JTextField(txt) {
    override fun updateUI() {
      super.updateUI()
      val fg = UIManager.getColor("TextField.foreground")
      foreground = Color(0x0, true)
      selectedTextColor = Color(0x0, true)
      val painter0 = ForegroundPainter(fg)
      val painter1 = ForegroundPainter(Color.RED)
      highlighter.removeAllHighlights()
      runCatching {
        highlighter.addHighlight(txt.indexOf("quick"), txt.indexOf("brown"), painter1)
        highlighter.addHighlight(0, txt.length, painter0)
      }.onFailure {
        it.printStackTrace()
      }
    }
  }
  val p = JPanel(BorderLayout())
  p.add(field)
  return p
}

private fun makePasswordPanel(): JPanel {
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

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.setBorder(BorderFactory.createTitledBorder(title))
  p.add(c)
  return p
}

private class DigitHighlightPasswordField(
  columns: Int,
) : JPasswordField(columns) {
  override fun setEchoChar(c: Char) {
    super.setEchoChar(c)
    val hasCaret = caret != null
    val start = if (hasCaret) getSelectionStart() else 0
    val end = if (hasCaret) getSelectionEnd() else 0
    (document as? AbstractDocument)?.also {
      updateHighlightFilter(it)
    }
    if (hasCaret) {
      selectionStart = start
      selectionEnd = end
    }
  }

  private fun updateHighlightFilter(doc: AbstractDocument) {
    if (echoChar == '\u0000') {
      setForeground(Color(0x0, true))
      setSelectedTextColor(Color(0x0, true))
      doc.documentFilter = HighlightFilter(this)
      runCatching {
        doc.remove(0, 0)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(this)
      }
    } else {
      highlighter?.removeAllHighlights()
      setForeground(UIManager.getColor("PasswordField.foreground"))
      setSelectedTextColor(UIManager.getColor("PasswordField.selectionForeground"))
      doc.documentFilter = null
    }
  }
}

private class HighlightFilter(
  private val field: JTextComponent,
) : DocumentFilter() {
  private val fgc = UIManager.getColor("PasswordField.foreground")
  private val defPainter = ForegroundPainter(fgc)
  private val numPainter = ForegroundPainter(Color.RED)
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
        highlighter.addHighlight(matcher.start(), pos, numPainter)
      }
      highlighter.addHighlight(0, doc.length, defPainter)
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(field)
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

private class ForegroundPainter(
  color: Color,
) : DefaultHighlightPainter(color) {
  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View,
  ): Shape {
    val r = getDrawingArea(offs0, offs1, bounds, view)
    if (!r.isEmpty) {
      runCatching {
        val s = c.document.getText(offs0, offs1 - offs0)
        val g2 = g.create() as Graphics2D
        val font = c.getFont()
        val metrics = g2.getFontMetrics(font)
        val ascent = metrics.ascent
        g2.color = color
        g2.drawString(s, r.x.toFloat(), (r.y + ascent).toFloat())
        g2.dispose()
      }.onFailure {
        it.printStackTrace()
      }
    }
    return r
  }

  // @see javax.swing.text.DefaultHighlighter.DefaultHighlightPainter#paintLayer(...)
  private fun getDrawingArea(
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    view: View,
  ) = if (offs0 == view.startOffset && offs1 == view.endOffset) {
    // Contained in view, can just use bounds.
    bounds as? Rectangle ?: bounds.bounds
  } else {
    // Should only render part of View.
    runCatching {
      // --- determine locations ---
      val shape = view.modelToView(
        offs0,
        Position.Bias.Forward,
        offs1,
        Position.Bias.Backward,
        bounds,
      )
      shape as? Rectangle ?: shape.bounds
    }.getOrNull() ?: Rectangle()
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
