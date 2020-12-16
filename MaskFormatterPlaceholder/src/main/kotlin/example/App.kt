package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.MaskFormatter

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)

  val mask = "###-####"
  val field0 = JFormattedTextField()
  runCatching { MaskFormatter(mask) }.getOrNull()?.also {
    field0.formatterFactory = DefaultFormatterFactory(it)
  }
  box.add(makeTitledPanel("new MaskFormatter(\"###-####\")", field0))
  box.add(Box.createVerticalStrut(15))

  val field1 = JFormattedTextField()
  runCatching { MaskFormatter(mask) }.getOrNull()?.also {
    it.placeholderCharacter = '_'
    field1.formatterFactory = DefaultFormatterFactory(it)
  }
  box.add(makeTitledPanel("MaskFormatter#setPlaceholderCharacter('_')", field1))
  box.add(Box.createVerticalStrut(15))

  val field2 = JFormattedTextField()
  runCatching { MaskFormatter(mask) }.getOrNull()?.also {
    it.placeholderCharacter = '_'
    it.placeholder = "000-0000"
    field2.formatterFactory = DefaultFormatterFactory(it)
  }
  box.add(makeTitledPanel("MaskFormatter#setPlaceholder(\"000-0000\")", field2))
  box.add(Box.createVerticalGlue())

  val font = Font(Font.MONOSPACED, Font.PLAIN, 18)
  val insets = Insets(1, 1 + 18 / 2, 1, 1)
  listOf(field0, field1, field2).forEach {
    it.font = font
    it.columns = mask.length + 1
    it.margin = insets
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel().also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
