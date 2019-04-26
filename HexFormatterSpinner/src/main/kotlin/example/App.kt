package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.text.ParseException
import java.util.EnumSet
import java.util.regex.Pattern
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory

class MainPanel : JPanel(BorderLayout()) {
  // Character.MIN_CODE_POINT: 0x0, Character.MAX_CODE_POINT: 0x10FFFF
  private val nm = SpinnerNumberModel(0x51DE, 0x0, Character.MAX_CODE_POINT, 1)
  private val spinner = JSpinner(nm)
  private val fontPanel = GlyphPaintPanel()
  var fontPaintFlag: Set<FontPaint> = EnumSet.allOf(FontPaint::class.java)

  // protected// , 0, len);
  val characterString: String
    get() {
      val code = spinner.getValue() as Int
      return String(Character.toChars(code))
    }

  init {
    spinner.addChangeListener { fontPanel.repaint() }
    val editor = spinner.getEditor() as JSpinner.NumberEditor
    val ftf = editor.getTextField() as JFormattedTextField
    ftf.setFont(Font(Font.MONOSPACED, Font.PLAIN, ftf.getFont().getSize()))
    ftf.setFormatterFactory(makeFFactory())

    val exMi = JRadioButton(FontPaint.IPA_EX_MINCHO.toString())
    exMi.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        setFontPaintFlag(EnumSet.of(FontPaint.IPA_EX_MINCHO))
      }
    }

    val mjMi = JRadioButton(FontPaint.IPA_MJ_MINCHO.toString())
    mjMi.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        setFontPaintFlag(EnumSet.of(FontPaint.IPA_MJ_MINCHO))
      }
    }

    val both = JRadioButton("Both", true)
    both.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        setFontPaintFlag(EnumSet.allOf(FontPaint::class.java))
      }
    }

    val p = JPanel()
    val bg = ButtonGroup()
    listOf(exMi, mjMi, both).forEach {
      p.add(it)
      bg.add(it)
    }

    add(spinner, BorderLayout.NORTH)
    add(fontPanel)
    add(p, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  protected fun setFontPaintFlag(fp: EnumSet<FontPaint>) {
    fontPaintFlag = fp
    fontPanel.repaint()
  }

  private inner class GlyphPaintPanel : JPanel() {
    private val ipaEx = Font("IPAexMincho", Font.PLAIN, 200)
    private val ipaMj = Font("IPAmjMincho", Font.PLAIN, 200)

    protected override fun paintComponent(g: Graphics) {
      val g2 = g.create() as Graphics2D
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setPaint(Color.WHITE)
      g2.fillRect(0, 0, getWidth(), getHeight())

      val str = characterString

      val frc = g2.getFontRenderContext()
      val exShape = TextLayout(str, ipaEx, frc).getOutline(null)
      val mjShape = TextLayout(str, ipaMj, frc).getOutline(null)

      val b = exShape.getBounds2D()
      val cx = getWidth() / 2.0 - b.getCenterX()
      val cy = getHeight() / 2.0 - b.getCenterY()
      val toCenterAtf = AffineTransform.getTranslateInstance(cx, cy)

      g2.setPaint(Color.YELLOW)
      g2.draw(toCenterAtf.createTransformedShape(b))

      val s1 = toCenterAtf.createTransformedShape(exShape)
      val s2 = toCenterAtf.createTransformedShape(mjShape)

      if (fontPaintFlag.contains(FontPaint.IPA_EX_MINCHO)) {
        g2.setPaint(Color.CYAN)
        g2.fill(s1)
      }
      if (fontPaintFlag.contains(FontPaint.IPA_MJ_MINCHO)) {
        g2.setPaint(Color.MAGENTA)
        g2.fill(s2)
      }
      if (fontPaintFlag.containsAll(EnumSet.allOf(FontPaint::class.java))) {
        g2.setClip(s1)
        g2.setPaint(Color.BLACK)
        g2.fill(s2)
      }
      g2.dispose()
    }
  }

  private fun makeFFactory(): DefaultFormatterFactory {
    val formatter = object : DefaultFormatter() {
      @Throws(ParseException::class)
      override fun stringToValue(text: String): Any {
        val pattern = Pattern.compile("^\\s*(\\p{XDigit}{1,6})\\s*$")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
          val iv = Integer.valueOf(text, 16)
          if (iv <= Character.MAX_CODE_POINT) {
            return iv
          }
        }
        Toolkit.getDefaultToolkit().beep()
        throw ParseException(text, 0)
      }

      @Throws(ParseException::class)
      override fun valueToString(value: Any?) = String.format("%06X", value as Int?)
    }
    formatter.setValueClass(Int::class.java)
    formatter.setOverwriteMode(true)
    return DefaultFormatterFactory(formatter)
  }
}

enum class FontPaint {
  IPA_EX_MINCHO, IPA_MJ_MINCHO
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
