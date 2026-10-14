package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.Element
import javax.swing.text.StyleConstants
import javax.swing.text.ViewFactory
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.ImageView

fun createUI(): Component {
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
    val defaultFactory = super.getViewFactory()
    return ViewFactory { elem ->
      if (isImg(elem)) {
        BaselineImageView(elem)
      } else {
        defaultFactory.create(elem)
      }
    }
  }

  private fun isImg(elem: Element): Boolean {
    val attrs = elem.attributes
    val name = attrs.getAttribute(AbstractDocument.ElementNameAttribute)
    return name == null && attrs.getAttribute(StyleConstants.NameAttribute) === HTML.Tag.IMG
  }
}

private class BaselineImageView(
  elem: Element,
) : ImageView(elem) {
  // .8125f magic number
  override fun getAlignment(axis: Int) =
    if (axis == Y_AXIS) .8125f else super.getAlignment(axis)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
