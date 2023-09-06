package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.text.ParseException
import java.util.EnumSet
import javax.swing.*
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory

// Character.MIN_CODE_POINT: 0x0, Character.MAX_CODE_POINT: 0x10FFFF
private val nm = SpinnerNumberModel(0x51DE, 0x0, Character.MAX_CODE_POINT, 1)
private val spinner = JSpinner(nm)
private val fontPanel = GlyphPaintPanel()
private var fontPaintFlag: Set<FontPaint> = EnumSet.allOf(FontPaint::class.java)

fun makeUI(): Component {
  nm.addChangeListener { fontPanel.repaint() }
  val editor = spinner.editor as? JSpinner.NumberEditor
  editor?.textField?.also {
    it.font = Font(Font.MONOSPACED, Font.PLAIN, it.font.size)
    it.formatterFactory = makeFormatterFactory()
  }

  val exMi = JRadioButton("IPAexMincho")
  exMi.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      setFontPaintFlag(EnumSet.of(FontPaint.IPA_EX_MINCHO))
    }
  }

  val mjMi = JRadioButton("IPAmjMincho")
  mjMi.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      setFontPaintFlag(EnumSet.of(FontPaint.IPA_MJ_MINCHO))
    }
  }

  val both = JRadioButton("Both", true)
  both.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      setFontPaintFlag(EnumSet.allOf(FontPaint::class.java))
    }
  }

  val p = JPanel()
  val bg = ButtonGroup()
  listOf(exMi, mjMi, both).forEach {
    p.add(it)
    bg.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(spinner, BorderLayout.NORTH)
    it.add(fontPanel)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getCharacterString() = String(Character.toChars(nm.number.toInt()))

private fun setFontPaintFlag(fp: EnumSet<FontPaint>) {
  fontPaintFlag = fp
  fontPanel.repaint()
}

private class GlyphPaintPanel : JPanel() {
  private val ipaEx = Font("IPAexMincho", Font.PLAIN, 200)
  private val ipaMj = Font("IPAmjMincho", Font.PLAIN, 200)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = Color.WHITE
    g2.fillRect(0, 0, width, height)

    val str = getCharacterString()

    val frc = g2.fontRenderContext
    val exShape = TextLayout(str, ipaEx, frc).getOutline(null)
    val mjShape = TextLayout(str, ipaMj, frc).getOutline(null)

    val b = exShape.bounds2D
    val cx = width / 2.0 - b.centerX
    val cy = height / 2.0 - b.centerY
    val toCenterAtf = AffineTransform.getTranslateInstance(cx, cy)

    g2.paint = Color.YELLOW
    g2.draw(toCenterAtf.createTransformedShape(b))

    val s1 = toCenterAtf.createTransformedShape(exShape)
    val s2 = toCenterAtf.createTransformedShape(mjShape)

    if (fontPaintFlag.contains(FontPaint.IPA_EX_MINCHO)) {
      g2.paint = Color.CYAN
      g2.fill(s1)
    }
    if (fontPaintFlag.contains(FontPaint.IPA_MJ_MINCHO)) {
      g2.paint = Color.MAGENTA
      g2.fill(s2)
    }
    if (fontPaintFlag.containsAll(EnumSet.allOf(FontPaint::class.java))) {
      g2.clip = s1
      g2.paint = Color.BLACK
      g2.fill(s2)
    }
    g2.dispose()
  }
}

private fun makeFormatterFactory(): DefaultFormatterFactory {
  val formatter = object : DefaultFormatter() {
    @Throws(ParseException::class)
    override fun stringToValue(text: String): Any {
      val regex = """^\s*(\p{XDigit}{1,6})\s*$""".toRegex()
      regex.find(text)?.also {
        val iv = Integer.valueOf(it.value, 16)
        if (iv <= Character.MAX_CODE_POINT) {
          return iv
        }
      }
      Toolkit.getDefaultToolkit().beep()
      throw ParseException(text, 0)
    }

    @Throws(ParseException::class)
    override fun valueToString(value: Any?) = "%06X".format(value as? Int)
  }
  formatter.valueClass = Integer::class.java
  formatter.overwriteMode = true
  return DefaultFormatterFactory(formatter)
}

private enum class FontPaint {
  IPA_EX_MINCHO,
  IPA_MJ_MINCHO
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
