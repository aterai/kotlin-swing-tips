package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.TabSet
import javax.swing.text.TabStop

fun makeUI(): Component {
  val textPane = JTextPane()
  textPane.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  val fm = textPane.getFontMetrics(textPane.font)
  val charWidth = fm.charWidth('m').toFloat()
  val tabWidth = charWidth * 4f
  val tabs = arrayOfNulls<TabStop>(10)
  for (j in tabs.indices) {
    tabs[j] = TabStop((j + 1) * tabWidth)
  }
  val tabSet = TabSet(tabs)
  val attributes = textPane.getStyle(StyleContext.DEFAULT_STYLE)
  StyleConstants.setTabSet(attributes, tabSet)
  textPane.setParagraphAttributes(attributes, false)
  textPane.text = """
    JTextPane
    0123
    	4567
    		89ab
  """.trimIndent()

  val textArea = JTextArea()
  textArea.tabSize = 4
  textArea.text = """
    JTextArea
    0123
    	4567
    		89ab
  """.trimIndent()

  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(textArea))
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
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
