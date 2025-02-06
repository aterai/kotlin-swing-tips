package example

import java.awt.*
import java.awt.event.ActionEvent
import java.io.StringReader
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DefaultHighlighter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Element
import javax.swing.text.MutableAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit.ParserCallback
import javax.swing.text.html.parser.ParserDelegator

private val highlightPainter = DefaultHighlightPainter(Color.YELLOW)
private val textArea = JTextArea()
private val editorPane = JEditorPane()
private val field = JTextField("3")
private val elementIdAction = object : AbstractAction("Element#getElement(id)") {
  override fun actionPerformed(e: ActionEvent) {
    textArea.append("----\n1:${getValue(NAME)}\n")
    val id = field.text.trim()
    val doc = editorPane.document as? HTMLDocument
    val element = doc?.getElement(id)
    if (element != null) {
      textArea.append("found: $element\n")
      editorPane.requestFocusInWindow()
      editorPane.select(element.startOffset, element.endOffset)
    }
  }
}
private val highlightAction = object : AbstractAction("Highlight Element[@id]") {
  override fun actionPerformed(e: ActionEvent) {
    textArea.append("----\n2:${getValue(NAME)}\n")
    if ((e.source as? JToggleButton)?.isSelected == true) {
      for (root in editorPane.document.rootElements) {
        traverseElementById(root)
      }
    } else {
      editorPane.highlighter.removeAllHighlights()
    }
  }
}
private val parserAction = object : AbstractAction("ParserDelegator") {
  override fun actionPerformed(e: ActionEvent) {
    textArea.append("----\n3:${getValue(NAME)}\n")
    val id = field.text.trim()
    val text = editorPane.text
    val delegator = ParserDelegator()
    runCatching {
      val cb = object : ParserCallback() {
        override fun handleStartTag(
          tag: HTML.Tag,
          a: MutableAttributeSet,
          pos: Int,
        ) {
          val attrId = a.getAttribute(HTML.Attribute.ID)
          textArea.append("$tag@id=$attrId\n")
          if (id == attrId) {
            textArea.append("found: pos=$pos\n")
            val endOffs = text.indexOf('>', pos)
            textArea.append("${text.substring(pos, endOffs + 1)}\n")
          }
        }
      }
      delegator.parse(StringReader(text), cb, true)
    }.onFailure {
      textArea.append("${it.message}\n")
      UIManager.getLookAndFeel().provideErrorFeedback(textArea)
    }
  }
}

fun makeUI(): Component {
  editorPane.editorKit = JEditorPane.createEditorKitForContentType("text/html")
  editorPane.text = """
    <html>
      12<span id='2'>345678</span>90
      <p>
        1
        <a href='..'>23</a>
        45
        <span class='insert' id='0'>6</span>
        7
        <span id='1'>8</span>
        90
        <div class='fff' id='3'>123</div>4567890
      </p>
    </html>
  """.trimMargin()
  (editorPane.highlighter as? DefaultHighlighter)?.drawsLayeredHighlights = false

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
    it.topComponent = JScrollPane(editorPane)
    it.bottomComponent = JScrollPane(textArea)
  }

  val p = JPanel(GridLayout(2, 2, 5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(field)
    it.add(JButton(elementIdAction))
    it.add(JToggleButton(highlightAction))
    it.add(JButton(parserAction))
  }

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addHighlight(
  element: Element,
  isBlock: Boolean,
) {
  val start = element.startOffset
  val lf = if (isBlock) 1 else 0
  val end = element.endOffset - lf
  runCatching {
    editorPane.highlighter.addHighlight(start, end, highlightPainter)
  }
}

private fun traverseElementById(element: Element) {
  if (element.isLeaf) {
    checkId(element)
  } else {
    for (i in 0..<element.elementCount) {
      val child = element.getElement(i)
      checkId(child)
      if (!child.isLeaf) {
        traverseElementById(child)
      }
    }
  }
}

private fun checkId(element: Element) {
  val attrs = element.attributes
  val elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute)
  val name = if (elementName == null) {
    attrs.getAttribute(StyleConstants.NameAttribute)
  } else {
    null
  }
  val tag = if (name is HTML.Tag) name else return
  textArea.append("$tag\n")
  if (tag.isBlock) { // block
    blockHighlight(element, attrs)
  } else { // inline
    inlineHighlight(element, attrs)
  }
}

private fun blockHighlight(
  element: Element,
  attrs: AttributeSet,
) {
  attrs.getAttribute(HTML.Attribute.ID)?.also {
    textArea.append("block: id=$it\n")
    addHighlight(element, true)
  }
}

private fun inlineHighlight(
  element: Element,
  attrs: AttributeSet,
) {
  attrs.attributeNames
    .toList()
    .map { attrs.getAttribute(it) }
    .filterIsInstance<AttributeSet>()
    .mapNotNull { it.getAttribute(HTML.Attribute.ID) }
    .forEach {
      textArea.append("inline: id=$it\n")
      addHighlight(element, false)
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
