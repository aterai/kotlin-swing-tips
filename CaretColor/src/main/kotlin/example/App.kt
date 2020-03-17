package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
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
  htmlEditorKit.setStyleSheet(styleSheet)
  val editor = JEditorPane()
  // editor.setEditable(false);
  editor.setEditorKit(htmlEditorKit)
  editor.setText(TEST_HTML)
  editor.setCaretColor(null)

  val field = JTextField("JTextField")
  field.setBackground(Color.GRAY)
  field.setForeground(Color.WHITE)

  val r1 = JRadioButton("RED")
  r1.addItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      field.setCaretColor(Color.RED)
      // editor.setCaretColor(Color.RED);
    }
  }

  val r2 = JRadioButton("null")
  r2.addItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      field.setCaretColor(null)
      // editor.setCaretColor(null);
    }
  }

  val r3 = JRadioButton("Lnf default", true)
  r3.addItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      val c = UIManager.getLookAndFeelDefaults().getColor("TextField.caretForeground")
      field.setCaretColor(c)
      // c = UIManager.getLookAndFeelDefaults().getColor("TextPane.caretForeground")
      // println(c)
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
  // area.setBackground(Color.GREEN);
  // area.setFont(area.getFont().deriveFont(15.0f));
  // area.setCaretColor(Color.RED);

  val p = JPanel(GridLayout(2, 1, 2, 2))
  p.add(JScrollPane(area))
  p.add(JScrollPane(editor))

  return JPanel(BorderLayout(5, 5)).also {
    it.setBorder(BorderFactory.createTitledBorder("JTextComponent#setCaretColor(...)"))
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.setPreferredSize(Dimension(320, 240))
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
