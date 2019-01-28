package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.html.FormSubmitEvent
import javax.swing.text.html.HTMLEditorKit

class MainPanel : JPanel(GridLayout(2, 1, 5, 5)) {
  init {
    val logger = JTextArea()
    logger.setEditable(false)

    val editor = JEditorPane().apply {
      val kit = HTMLEditorKit()
      kit.setAutoFormSubmission(false)
      setEditorKit(kit)
      setEditable(false)
    }

    val form = "<form action='#'><input type='text' name='word' value='12345' /></form>"
    editor.setText("<html><h1>Form test</h1>$form")
    editor.addHyperlinkListener({ e ->
      if (e is FormSubmitEvent) {
        val data = (e as FormSubmitEvent).getData()
        logger.append(data + "\n")

        val charset = Charset.defaultCharset().toString()
        logger.append("default charset: $charset\n")

        try {
          val txt = URLDecoder.decode(data, charset)
          logger.append(txt + "\n")
        } catch (ex: UnsupportedEncodingException) {
          ex.printStackTrace()
          logger.append(ex.message + "\n")
        }
      }
    })

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(editor))
    add(JScrollPane(logger))
    setPreferredSize(Dimension(320, 240))
  }
}

fun main() {
  EventQueue.invokeLater({
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  })
}
