package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

private val jtp = JTextPane()

private fun append(str: String, flg: Boolean) {
  val style = if (flg) StyleContext.DEFAULT_STYLE else "error"
  val doc = jtp.styledDocument
  runCatching {
    doc.insertString(doc.length, str, doc.getStyle(style))
  }
}

fun makeUI(): Component {
  val ok = JButton("Test")
  ok.addActionListener { append("Test test test test\n", true) }

  val err = JButton("Error")
  err.addActionListener { append("Error error error error\n", false) }

  val clr = JButton("Clear")
  clr.addActionListener { jtp.text = "" }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(ok)
  box.add(err)
  box.add(Box.createHorizontalStrut(5))
  box.add(clr)

  jtp.isEditable = false
  val doc = jtp.styledDocument
  val def = doc.getStyle(StyleContext.DEFAULT_STYLE)
  val error = doc.addStyle("error", def)
  StyleConstants.setForeground(error, Color.RED)

  val scroll = JScrollPane(jtp)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  scroll.verticalScrollBar.unitIncrement = 25

  return JPanel(BorderLayout(5, 5)).also {
    it.add(scroll)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
