package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class MainPanel : JPanel(BorderLayout()) {
  init {
    val styleSheet = StyleSheet().also {
      it.addRule("body {font-size: 12pt;}")
      it.addRule(".highlight {color: red; background: green}")
    }
    val htmlEditorKit = HTMLEditorKit()
    htmlEditorKit.setStyleSheet(styleSheet)
    // val htmlDocument = (HTMLDocument) htmlEditorKit.createDefaultDocument()
    val editor = JEditorPane()
    editor.setEditorKit(htmlEditorKit)
    // editor.setDocument(htmlDocument)
    editor.setText(makeTestHtml())
    add(JScrollPane(editor))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTestHtml() = """
    <html>
      <body>
        <div>0000000</div>
        <div class='highlight'>1111111111</div>
        <div>2222222222</div>
      </body>
    </html>
    """
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
