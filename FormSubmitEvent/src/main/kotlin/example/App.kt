package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.net.URLDecoder
import java.nio.charset.Charset
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.FormSubmitEvent
import javax.swing.text.html.HTMLEditorKit

fun makeUI(): Component {
  val logger = JTextArea()
  logger.setEditable(false)

  val editor = JEditorPane().also {
    val kit = HTMLEditorKit()
    kit.setAutoFormSubmission(false)
    it.setEditorKit(kit)
    it.setEditable(false)
  }

  val form = "<form action='#'><input type='text' name='word' value='12345' /></form>"
  editor.setText("<html><h1>Form test</h1>$form")
  editor.addHyperlinkListener { e ->
    // if (e is FormSubmitEvent) {
    val data = (e as? FormSubmitEvent)?.getData() ?: return@addHyperlinkListener
    logger.append(data + "\n")

    val charset = Charset.defaultCharset().toString()
    logger.append("default charset: $charset\n")

    runCatching {
      logger.append(URLDecoder.decode(data, charset) + "\n")
    }.onFailure {
      logger.append(it.message + "\n")
    }
  }

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.add(JScrollPane(editor))
    it.add(JScrollPane(logger))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
