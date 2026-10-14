package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.Element
import javax.swing.text.ParagraphView
import javax.swing.text.Position
import javax.swing.text.StyledEditorKit
import javax.swing.text.ViewFactory

fun createUI(): Component {
  val editor = JEditorPane()
  editor.editorKit = ParagraphMarkEditorKit()
  editor.text = """
    1234123541341234123423
    12374612340
    213441324

    645206345437820
  """.trimIndent()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ParagraphMarkEditorKit : StyledEditorKit() {
  override fun getViewFactory(): ViewFactory {
    // Replace only the paragraph view and delegate the other elements
    // to the default StyledEditorKit factory
    val defaultFactory = super.getViewFactory()
    return ViewFactory { elem ->
      if (AbstractDocument.ParagraphElementName == elem.name) {
        ParagraphMarkView(elem)
      } else {
        defaultFactory.create(elem)
      }
    }
  }
}

private class ParagraphMarkView(
  elem: Element?,
) : ParagraphView(elem) {
  override fun paint(
    g: Graphics,
    allocation: Shape,
  ) {
    super.paint(g, allocation)
    paintParagraphMark(g, allocation)
  }

  private fun paintParagraphMark(
    g: Graphics,
    a: Shape,
  ) {
    runCatching {
      // Backward bias keeps the mark on the last row of a wrapped paragraph
      val eop = modelToView(endOffset, a, Position.Bias.Backward)
      val r = eop?.bounds ?: a.bounds
      val x = r.x
      val y = r.y
      val h = r.height
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = MARK_COLOR
      g2.drawLine(x + 1, y + h / 2, x + 1, y + h - 4)
      g2.drawLine(x + 2, y + h / 2, x + 2, y + h - 5)
      g2.drawLine(x + 3, y + h - 6, x + 3, y + h - 6)
      g2.dispose()
    }
  }

  companion object {
    private val MARK_COLOR = Color(0x78_82_6E)
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
