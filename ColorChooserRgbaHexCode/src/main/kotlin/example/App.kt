package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.text.ParseException
import javax.swing.*
import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.DocumentFilter

fun makeUI(): Component {
  val label = object : JLabel() {
    override fun getPreferredSize() = Dimension(32, 32)
  }
  label.setOpaque(true)
  label.setBackground(Color.RED)
  UIManager.put("ColorChooser.rgbHexCodeText", "#RGBA:")
  val button = JButton("open JColorChooser")
  button.addActionListener {
    val cc = JColorChooser()
    cc.color = Color(0xFF_FF_00_00.toInt(), true)
    val panels = cc.chooserPanels
    val choosers = panels.toMutableList()
    val ccp = choosers[3]
    // Java 9: if (ccp.isColorTransparencySelectionEnabled()) {
    for (c in ccp.components) {
      if (c is JFormattedTextField) {
        removeFocusListeners(c)
        init(c)
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
    val color = ok.color
    if (color != null) {
      label.setBackground(color)
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
  text.setColumns(8)
  text.setFormatterFactory(DefaultFormatterFactory(formatter))
  text.setHorizontalAlignment(SwingConstants.RIGHT)
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
    override fun remove(fb: FilterBypass, offset: Int, length: Int) {
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
  override fun stringToValue(text: String): Any {
    return try {
      val r = text.substring(0, 2).toInt(16)
      val g = text.substring(2, 4).toInt(16)
      val b = text.substring(4, 6).toInt(16)
      val a = text.substring(6).toInt(16)
      a shl 24 or (r shl 16) or (g shl 8) or b
    } catch (nfe: NumberFormatException) {
      val pe = ParseException("illegal format", 0)
      pe.initCause(nfe)
      throw pe
    }
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
      val argb = String(array).uppercase()
      return argb.substring(2) + argb.substring(0, 2)
    }
    throw ParseException("illegal object", 0)
  }

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