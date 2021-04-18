package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

private const val TEST_HTML = """
  <html>
    <body>
      <div>0000000</div>
      <div class='highlight'>1111111111</div>
      <div>2222222222</div>
    </body>
  </html>
"""

fun makeUI(): Component {
  val styleSheet = StyleSheet().also {
    it.addRule("body {font-size: 12pt;}")
    it.addRule(".highlight {color: red; background: green}")
  }
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet
  // val htmlDocument = (HTMLDocument) htmlEditorKit.createDefaultDocument()
  val editor = JEditorPane()
  editor.editorKit = htmlEditorKit
  // editor.setDocument(htmlDocument)
  editor.text = TEST_HTML
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
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
