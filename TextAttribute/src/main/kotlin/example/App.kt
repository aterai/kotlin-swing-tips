package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.font.TextAttribute
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

private const val TEXT = "0123456789"
private val textField0 = JTextField(TEXT)
private val textField1 = JTextField(TEXT)
private val textField2 = JTextField(TEXT)
private val textArea = JTextArea("$TEXT\n$TEXT\n")

fun makeUI(): Component {
  val font = textField2.font
  textField2.font = font.deriveFont(16f)

  val comboBox = JComboBox(UnderlineStyle.values())
  comboBox.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is UnderlineStyle) {
      val style = item.style
      initUnderline(textField0, style)
      initUnderline(textField1, style)
      initUnderline(textField2, style)
      initUnderline(textArea, style)
    }
  }

  val p = JPanel(GridLayout(3, 1, 5, 5))
  p.add(comboBox, BorderLayout.NORTH)
  p.add(textField1, BorderLayout.NORTH)
  p.add(textField2, BorderLayout.NORTH)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(textField0, BorderLayout.SOUTH)
    it.add(JScrollPane(textArea))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initUnderline(tc: JTextComponent, style: Any) {
  val font = tc.font
  val attrs = mutableMapOf<TextAttribute, Any?>()
  attrs.putAll(font.attributes)
  attrs[TextAttribute.UNDERLINE] = style
  tc.font = font.deriveFont(attrs)
}

private enum class UnderlineStyle(val style: Int) {
  UNDERLINE_OFF(-1),
  UNDERLINE_LOW_DASHED(TextAttribute.UNDERLINE_LOW_DASHED),
  UNDERLINE_LOW_DOTTED(TextAttribute.UNDERLINE_LOW_DOTTED),
  UNDERLINE_LOW_GRAY(TextAttribute.UNDERLINE_LOW_GRAY),
  UNDERLINE_LOW_ONE_PIXEL(TextAttribute.UNDERLINE_LOW_ONE_PIXEL),
  UNDERLINE_LOW_TWO_PIXEL(TextAttribute.UNDERLINE_LOW_TWO_PIXEL),
  UNDERLINE_ON(TextAttribute.UNDERLINE_ON);
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
