package example

import java.awt.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

private const val ROW_TXT = "<tr bgColor='%s'><td>%s</td><td>%s</td></tr>"
private const val TABLE_TXT = "<table id='log' border='1'></table>"
private const val BODY_TXT = "<html><body>head${TABLE_TXT}tail</body></html>"

fun makeUI(): Component {
  val htmlEditorKit = HTMLEditorKit()
  val editor = JEditorPane()
  editor.editorKit = htmlEditorKit
  editor.text = BODY_TXT
  editor.isEditable = false

  val insertAfterStart = JButton("insertAfterStart")
  insertAfterStart.addActionListener {
    (editor.document as? HTMLDocument)?.also {
      val element = it.getElement("log")
      val date = LocalDateTime.now(ZoneId.systemDefault())
      val tag = ROW_TXT.format("#AEEEEE", "insertAfterStart", date)
      runCatching {
        it.insertAfterStart(element, tag)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(editor)
      }
    }
  }

  val insertBeforeEnd = JButton("insertBeforeEnd")
  insertBeforeEnd.addActionListener {
    (editor.document as? HTMLDocument)?.also {
      val element = it.getElement("log")
      val date = LocalDateTime.now(ZoneId.systemDefault())
      val tag = ROW_TXT.format("#FFFFFF", "insertBeforeEnd", date)
      runCatching {
        it.insertBeforeEnd(element, tag)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(editor)
      }
    }
  }

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(Box.createHorizontalGlue())
  box.add(insertAfterStart)
  box.add(Box.createHorizontalStrut(5))
  box.add(insertBeforeEnd)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.add(box, BorderLayout.SOUTH)
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
