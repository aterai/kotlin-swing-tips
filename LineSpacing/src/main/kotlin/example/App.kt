package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.Element
import javax.swing.text.IconView
import javax.swing.text.LabelView
import javax.swing.text.MutableAttributeSet
import javax.swing.text.ParagraphView
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory

fun makeUI(): Component {
  val attr = SimpleAttributeSet()
  StyleConstants.setForeground(attr, Color.RED)
  StyleConstants.setFontSize(attr, 32)
  val a = SimpleAttributeSet()
  StyleConstants.setLineSpacing(a, .5f)

  val editor1 = JTextPane()
  editor1.setParagraphAttributes(a, false)
  setSampleText(editor1, attr)

  val editor2 = JTextPane()
  editor2.editorKit = BottomInsetEditorKit()
  setSampleText(editor2, attr)
  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(editor1)
  sp.bottomComponent = JScrollPane(editor2)
  sp.resizeWeight = .5

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setSampleText(textPane: JTextPane, attr: MutableAttributeSet) {
  textPane.text = "12341234\n1234 567890 5555 66666 77777\n88 999999 "
  runCatching {
    val doc = textPane.styledDocument
    doc.insertString(doc.length, "134500698\n", attr)
  }
}

private class BottomInsetEditorKit : StyledEditorKit() {
  override fun getViewFactory() = BottomInsetViewFactory()
}

private class BottomInsetViewFactory : ViewFactory {
  override fun create(elem: Element) = when (elem.name) {
    AbstractDocument.ParagraphElementName -> object : ParagraphView(elem) {
      override fun getBottomInset(): Short = 5
    }
    AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
    StyleConstants.ComponentElementName -> ComponentView(elem)
    StyleConstants.IconElementName -> IconView(elem)
    else -> LabelView(elem)
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
