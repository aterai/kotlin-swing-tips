package example

import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.Document
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledEditorKit

fun makeUI(): Component {
  val faceMark = ":)"
  val textPane = JTextPane()
  textPane.editorKit = StyledEditorKit()

  val dl = object : DocumentListener {
    override fun changedUpdate(e: DocumentEvent) {
      // not needed
    }

    override fun insertUpdate(e: DocumentEvent) {
      update(e.document, e.offset)
    }

    override fun removeUpdate(e: DocumentEvent) {
      update(e.document, e.offset)
    }

    private fun update(
      d: Document,
      offset: Int,
    ) {
      val doc = d as? DefaultStyledDocument ?: return
      val elm = doc.getCharacterElement(offset)
      EventQueue.invokeLater {
        runCatching {
          val start = elm.startOffset
          val end = elm.endOffset
          val text = doc.getText(start, end - start)
          var pos = text.indexOf(faceMark)
          while (pos > -1) {
            val face = doc.getStyle(faceMark)
            doc.setCharacterAttributes(start + pos, faceMark.length, face, false)
            pos = text.indexOf(faceMark, pos + faceMark.length)
          }
        }
      }
    }
  }

  val doc = textPane.styledDocument
  doc.addDocumentListener(dl)

  val face = doc.addStyle(faceMark, doc.getStyle(StyleContext.DEFAULT_STYLE))
  StyleConstants.setIcon(face, FaceIcon())
  textPane.text = "aaa 🙂 12345 :) 67890 :-) aaa\n"

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private class FaceIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.color = Color.RED
    g2.drawOval(1, 1, 14, 14)
    g2.drawLine(5, 10, 6, 10)
    g2.drawLine(7, 11, 9, 11)
    g2.drawLine(10, 10, 11, 10)
    g2.drawOval(4, 5, 1, 1)
    g2.drawOval(10, 5, 1, 1)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
