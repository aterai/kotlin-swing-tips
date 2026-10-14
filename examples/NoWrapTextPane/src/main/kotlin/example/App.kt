package example

import java.awt.*
import java.awt.event.ActionListener
import java.util.concurrent.Executors
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.Element
import javax.swing.text.ParagraphView
import javax.swing.text.StyledEditorKit
import javax.swing.text.ViewFactory

private var text: String? = null
private val threadPool = Executors.newCachedThreadPool().also {
  it.execute {
    text = "a".repeat(1024 * 1024 - 2) + "x\n"
  }
}

fun createUI(): Component {
  val editorPane = JEditorPane()
  val textArea = JTextArea()
  val editorPaneButton = JButton("JEditorPane")
  val textAreaButton = JButton("JTextArea")
  editorPane.editorKit = NoWrapEditorKit2()

  val longTextListener = ActionListener { e ->
    threadPool.execute {
      if (text != null) {
        if (editorPaneButton == e.source) {
          editorPane.text = text
        } else {
          textArea.text = text
        }
      }
    }
  }
  editorPaneButton.addActionListener(longTextListener)
  textAreaButton.addActionListener(longTextListener)

  val clearButton = JButton("clear all")
  clearButton.addActionListener {
    editorPane.text = ""
    textArea.text = ""
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(editorPaneButton)
  box.add(textAreaButton)
  box.add(clearButton)

  val p = JPanel(GridLayout(2, 1))
  p.add(makeTitledPanel("NoWrapEditorKit(JEditorPane)", editorPane))
  p.add(makeTitledPanel("JTextArea", textArea))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val sp = JScrollPane(c)
  sp.border = BorderFactory.createTitledBorder(title)
  return sp
}

private class NoWrapParagraphView(
  elem: Element,
) : ParagraphView(elem) {
  override fun calculateMinorAxisRequirements(
    axis: Int,
    r: SizeRequirements?,
  ): SizeRequirements {
    val req = super.calculateMinorAxisRequirements(axis, r)
    req.minimum = req.preferred
    return req
  }

  override fun getFlowSpan(index: Int) = Int.MAX_VALUE
}

private class NoWrapEditorKit2 : StyledEditorKit() {
  override fun getViewFactory(): ViewFactory {
    val defaultFactory = super.getViewFactory()
    return ViewFactory { elem ->
      if (AbstractDocument.ParagraphElementName == elem.name) {
        NoWrapParagraphView(elem)
      } else {
        defaultFactory.create(elem)
      }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
