package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.Element
import javax.swing.text.StyleConstants
import javax.swing.text.View
import javax.swing.text.ViewFactory
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.ImageView

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/16x16.png")
  val text = "<span>Hello <img src='$url' />!!!</span>"
  val editor1 = makeEditorPane(HTMLEditorKit(), text)
  val editor2 = makeEditorPane(ImgBaselineHtmlEditorKit(), text)

  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(editor1))
    it.add(JScrollPane(editor2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeEditorPane(
  kit: HTMLEditorKit,
  text: String,
) = JEditorPane().also {
  it.isEditable = false
  it.contentType = "text/html"
  it.editorKit = kit
  it.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  it.text = text

  val style = kit.styleSheet
  style.addRule("span {color: orange;}")
  style.addRule("img {align: middle; valign: middle; vertical-align: middle;}")
}

private class ImgBaselineHtmlEditorKit : HTMLEditorKit() {
  override fun getViewFactory(): ViewFactory {
    return object : HTMLFactory() {
      override fun create(elem: Element): View {
        // val view = super.create(elem)
        // if (view is LabelView) {
        //   println("debug: " + view.getAlignment(View.Y_AXIS))
        // }
        val attrs = elem.attributes
        val elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute)
        val o = if (elementName == null) {
          attrs.getAttribute(StyleConstants.NameAttribute)
        } else {
          null
        }
        return if (o is HTML.Tag && o === HTML.Tag.IMG) {
          createImageView(elem)
        } else {
          super.create(elem)
        }
      }
    }
  }

  private fun createImageView(elem: Element) =
    object : ImageView(elem) {
      override fun getAlignment(axis: Int) =
        if (axis == Y_AXIS) .8125f else super.getAlignment(axis) // .8125f magic number
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
