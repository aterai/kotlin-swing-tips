package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

private const val TEST_HTML = """
<html>
  <body>
    <div>JTextPane#setCaretColor(null)</div>
    <div class='highlight'>1111111111</div>
    <div>2222222222</div>
  </body>
</html>
"""

fun makeUI(): Component {
  // UIManager.put("TextPane.caretForeground", Color.ORANGE)
  val styleSheet = StyleSheet()
  styleSheet.addRule("body {font-size: 12pt}")
  styleSheet.addRule(".highlight {color: red; background: green}")
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet
  val editor = JEditorPane()
  editor.editorKit = htmlEditorKit
  editor.text = TEST_HTML
  editor.caretColor = null

  val field = JTextField("JTextField")
  field.background = Color.GRAY
  field.foreground = Color.WHITE

  val r1 = JRadioButton("RED")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      field.caretColor = Color.RED
    }
  }

  val r2 = JRadioButton("null")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      field.caretColor = null
    }
  }

  val r3 = JRadioButton("Lnf default", true)
  r3.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val c = UIManager.getLookAndFeelDefaults().getColor("TextField.caretForeground")
      field.caretColor = c
      // c = UIManager.getLookAndFeelDefaults().getColor("TextPane.caretForeground")
      // editor.setCaretColor(c)
    }
  }

  val bg = ButtonGroup()
  val box = Box.createHorizontalBox()
  listOf(r1, r2, r3).forEach {
    bg.add(it)
    box.add(it)
    box.add(Box.createHorizontalStrut(2))
  }
  box.add(field)

  UIManager.put("TextArea.caretForeground", Color.ORANGE)
  val area = JTextArea("TextArea.caretForeground: ORANGE")
  // area.background = Color.GREEN
  // area.font = area.font.deriveFont(15.0f)
  // area.caretColor = Color.RED

  val p = JPanel(GridLayout(2, 1, 2, 2))
  p.add(JScrollPane(area))
  p.add(JScrollPane(editor))

  return JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createTitledBorder("JTextComponent#setCaretColor(...)")
    it.add(box, BorderLayout.NORTH)
    it.add(p)
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
