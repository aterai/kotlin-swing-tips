package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.text.ParseException
import java.util.Locale
import javax.swing.*
import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.colorchooser.AbstractColorChooserPanel
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.DocumentFilter

fun makeUI(): Component {
  val label = object : JLabel() {
    override fun getPreferredSize() = Dimension(32, 32)
  }
  label.isOpaque = true
  label.background = Color(0xFF_FF_00_00.toInt(), true)
  UIManager.put("ColorChooser.rgbHexCodeText", "#RGBA:")
  val button = JButton("open JColorChooser")
  button.addActionListener {
    val cc = JColorChooser()
    cc.color = label.background
    val panels = cc.chooserPanels
    val choosers = panels.toMutableList()
    val ccp = getRgbChooser(cc)
    // Java 9: if (ccp.isColorTransparencySelectionEnabled() && ccp != null) {
    if (ccp != null) {
      for (c in ccp.components) {
        if (c is JFormattedTextField) {
          removeFocusListeners(c)
          init(c)
        }
      }
    }
    cc.setChooserPanels(choosers.toTypedArray())
    val ok = ColorTracker(cc)
    val parent = button.rootPane
    val title = "JColorChooser"
    val dialog = JColorChooser.createDialog(parent, title, true, cc, ok, null)
    dialog.addComponentListener(object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent) {
        (e.component as? Window)?.dispose()
      }
    })
    dialog.isVisible = true
    ok.color?.also {
      label.setBackground(it)
    }
  }
  return JPanel().also {
    it.add(label)
    it.add(button)
    it.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun init(text: JFormattedTextField) {
  val formatter = ValueFormatter()
  text.columns = 9
  text.setFont(Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()))
  text.formatterFactory = DefaultFormatterFactory(formatter)
  text.horizontalAlignment = SwingConstants.RIGHT
  text.minimumSize = text.getPreferredSize()
  text.addFocusListener(formatter)
}

private fun removeFocusListeners(c: Component) {
  for (l in c.focusListeners) {
    if (l is AbstractFormatter) {
      c.removeFocusListener(l)
    }
  }
}

private class ColorTracker(private val chooser: JColorChooser) : ActionListener {
  var color: Color? = null
    private set

  override fun actionPerformed(e: ActionEvent) {
    color = chooser.color
  }
}

private class ValueFormatter : AbstractFormatter(), FocusListener {
  private val filter: DocumentFilter = object : DocumentFilter() {
    @Throws(BadLocationException::class)
    override fun remove(
      fb: FilterBypass,
      offset: Int,
      length: Int,
    ) {
      if (isValidLength(fb.document.length - length)) {
        fb.remove(offset, length)
      }
    }

    @Throws(BadLocationException::class)
    override fun replace(
      fb: FilterBypass,
      offset: Int,
      length: Int,
      text: String,
      set: AttributeSet?,
    ) {
      if (isValidLength(fb.document.length + text.length - length) && isValid(text)) {
        fb.replace(offset, length, text.uppercase(), set)
      }
    }

    @Throws(BadLocationException::class)
    override fun insertString(
      fb: FilterBypass,
      offset: Int,
      text: String,
      set: AttributeSet?,
    ) {
      if (isValidLength(fb.document.length + text.length) && isValid(text)) {
        fb.insertString(offset, text.uppercase(), set)
      }
    }

    private fun isValidLength(len: Int) = 0 <= len && len <= 8

    private fun isValid(text: String): Boolean {
      val len = text.length
      for (i in 0 until len) {
        val ch = text[i]
        if (ch.digitToIntOrNull(16) ?: -1 < 0) {
          return false
        }
      }
      return true
    }
  }

  @Throws(ParseException::class)
  override fun stringToValue(text: String) = try {
    // val r = text.substring(0, 2).toInt(16)
    // val g = text.substring(2, 4).toInt(16)
    // val b = text.substring(4, 6).toInt(16)
    // val a = text.substring(6).toInt(16)
    // a shl 24 or (r shl 16) or (g shl 8) or b
    Integer.parseUnsignedInt(rgbaToArgb(text), 16)
  } catch (nfe: NumberFormatException) {
    val pe = ParseException("illegal format", 0)
    pe.initCause(nfe)
    throw pe
  }

  @Throws(ParseException::class)
  override fun valueToString(obj: Any): String {
    if (obj is Int) {
      var value: Int = obj
      val array = CharArray(8)
      for (i in array.indices.reversed()) {
        array[i] = Character.forDigit(value and 0x0F, 16)
        value = value shr 4
      }
      return argbToRgba(String(array).uppercase())
    }
    throw ParseException("illegal object", 0)
  }

  private fun argbToRgba(argb: String) = argb.substring(2) + argb.substring(0, 2)

  private fun rgbaToArgb(rgba: String) = rgba.substring(6) + rgba.substring(0, 6)

  override fun getDocumentFilter() = filter

  override fun focusGained(e: FocusEvent) {
    val source = e.source
    if (source is JFormattedTextField) {
      SwingUtilities.invokeLater { source.selectAll() }
    }
  }

  override fun focusLost(e: FocusEvent) {
    // Do nothing
  }
}

private fun getRgbChooser(colorChooser: JColorChooser): AbstractColorChooserPanel? {
  val rgbName = UIManager.getString("ColorChooser.rgbNameText", Locale.getDefault())
  var rgbChooser: AbstractColorChooserPanel? = null
  for (p in colorChooser.chooserPanels) {
    if (rgbName == p.displayName) {
      rgbChooser = p
    }
  }
  return rgbChooser
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
