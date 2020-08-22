package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.Element
import javax.swing.text.Position.Bias
import javax.swing.text.StyleConstants
import javax.swing.text.View
import javax.swing.text.html.BlockView
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

private val cl = Thread.currentThread().contextClassLoader
private val src = cl.getResource("example/favicon.png")
private val HTML_TEXT = """
  <html><body>
  span tag: <span style='background:#88ff88;' title='tooltip: span[@title]'>span span span</span><br />
  <div title='tooltip: div[@title]'>div tag: div div div div</div>
  <div style='padding: 2 24;'><img src='$src' alt='16x16 favicon' />&nbsp;
  <a href='https://ateraimemo.com/' title='Title: JST'>Java Swing Tips</a></div>
  </body></html>
"""
private var tooltip: String? = null

fun makeUI(): Component {
  val editor1 = CustomTooltipEditorPane()
  editor1.editorKit = HTMLEditorKit()
  editor1.text = HTML_TEXT
  editor1.isEditable = false
  ToolTipManager.sharedInstance().registerComponent(editor1)

  val editor2 = JEditorPane()
  editor2.editorKit = TooltipEditorKit()
  editor2.text = HTML_TEXT
  editor2.isEditable = false
  editor2.addHyperlinkListener { e ->
    (e.source as? JEditorPane)?.also { editorPane ->
      when (e.eventType) {
        HyperlinkEvent.EventType.ACTIVATED ->
          JOptionPane.showMessageDialog(editorPane, "You click the link with the URL " + e.url)
        HyperlinkEvent.EventType.ENTERED -> {
          tooltip = editorPane.toolTipText
          editorPane.toolTipText = e.url?.toExternalForm()
        }
        HyperlinkEvent.EventType.EXITED -> editorPane.toolTipText = tooltip
      }
    }
  }
  ToolTipManager.sharedInstance().registerComponent(editor2)

  return JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
    it.resizeWeight = .5
    it.topComponent = JScrollPane(editor1)
    it.bottomComponent = JScrollPane(editor2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CustomTooltipEditorPane : JEditorPane() {
  @Transient
  private val bias = arrayOfNulls<Bias>(1)

  @Transient
  private var listener: HyperlinkListener? = null

  override fun updateUI() {
    removeHyperlinkListener(listener)
    super.updateUI()
    listener = object : HyperlinkListener {
      private var tooltip: String? = null
      override fun hyperlinkUpdate(e: HyperlinkEvent) {
        (e.source as? JEditorPane)?.also { editor ->
          when (e.eventType) {
            HyperlinkEvent.EventType.ACTIVATED -> JOptionPane.showMessageDialog(editor, e.url)
            HyperlinkEvent.EventType.ENTERED -> {
              tooltip = editor.toolTipText
              (e.sourceElement?.attributes?.getAttribute(HTML.Tag.A) as? AttributeSet)?.also {
                editor.toolTipText = it.getAttribute(HTML.Attribute.TITLE)?.toString()
              }
            }
            HyperlinkEvent.EventType.EXITED -> editor.toolTipText = tooltip
          }
        }
      }
    }
    addHyperlinkListener(listener)
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val title = super.getToolTipText(e)
    val editor = e.component as JEditorPane
    if (!editor.isEditable) {
      var pos = editor.ui.viewToModel(editor, e.point, bias)
      if (bias[0] == Bias.Backward && pos > 0) {
        pos--
      }
      if (pos >= 0 && editor.document is HTMLDocument) {
        val doc = editor.document as HTMLDocument
        return getSpanTitleAttribute(doc, pos) ?: title
      }
    }
    return title
  }

  private fun getSpanTitleAttribute(doc: HTMLDocument, pos: Int): String? {
    val a = doc.getCharacterElement(pos).attributes
    val span = a.getAttribute(HTML.Tag.SPAN) as? AttributeSet
    return span?.getAttribute(HTML.Attribute.TITLE)?.toString()
  }
}

private class TooltipEditorKit : HTMLEditorKit() {
  override fun getViewFactory() = object : HTMLFactory() {
    override fun create(elem: Element): View {
      val attrs = elem.attributes
      val elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute)
      val o = if (elementName == null) attrs.getAttribute(StyleConstants.NameAttribute) else null
      if (o is HTML.Tag && o === HTML.Tag.DIV) {
        return object : BlockView(elem, Y_AXIS) {
          override fun getToolTipText(x: Float, y: Float, allocation: Shape) =
            super.getToolTipText(x, y, allocation) ?:
            element?.attributes?.getAttribute(HTML.Attribute.TITLE)?.toString()
        }
      }
      return super.create(elem)
    }
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
