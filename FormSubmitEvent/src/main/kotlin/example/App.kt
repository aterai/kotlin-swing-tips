package example

import java.awt.*
import java.net.URLDecoder
import java.nio.charset.Charset
import javax.swing.*
import javax.swing.text.html.FormSubmitEvent
import javax.swing.text.html.HTMLEditorKit

fun makeUI(): Component {
  val logger = JTextArea()
  logger.isEditable = false

  val editor = JEditorPane().also {
    val kit = HTMLEditorKit()
    kit.isAutoFormSubmission = false
    it.editorKit = kit
    it.isEditable = false
  }

  val form = "<form action='#'><input type='text' name='word' value='12345' /></form>"
  editor.text = "<html><h1>Form test</h1>$form"
  editor.addHyperlinkListener { e ->
    val data = (e as? FormSubmitEvent)?.data
    if (data != null) {
      logger.append(data + "\n")

      val charset = Charset.defaultCharset().toString()
      logger.append("default charset: $charset\n")

      runCatching {
        logger.append(URLDecoder.decode(data, charset) + "\n")
      }.onFailure {
        logger.append(it.message + "\n")
      }
    }
  }

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(editor))
    it.add(JScrollPane(logger))
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
