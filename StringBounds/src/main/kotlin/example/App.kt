package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private val field = JTextField("The quick brown fox")
private val label = JLabel(field.text)
private val log = JTextArea()

fun makeUI(): Component {
  log.isEditable = false
  field.document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      update()
    }

    override fun removeUpdate(e: DocumentEvent) {
      update()
    }

    override fun changedUpdate(e: DocumentEvent) {
      update()
    }
  })
  update()

  val panel = JPanel(GridLayout(2, 1, 5, 5))
  panel.add(field)
  panel.add(label)

  return JPanel(BorderLayout()).also {
    it.add(panel, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun update() {
  log.text = ""
  val txt = field.text.trim()
  label.text = txt
  if (txt.isEmpty()) {
    return
  }
  val font = label.font
  val frc = label.getFontMetrics(font).fontRenderContext
  append("Font#getStringBounds(...)", font.getStringBounds(txt, frc))

  val layout = TextLayout(txt, font, frc)
  append("TextLayout#getBounds()", layout.bounds)

  val gv = font.createGlyphVector(frc, txt)
  append("GlyphVector#getPixelBounds(...)", gv.getPixelBounds(frc, 0f, 0f))
  append("GlyphVector#getLogicalBounds()", gv.logicalBounds)
  append("GlyphVector#getVisualBounds()", gv.visualBounds)

  append("JLabel#getPreferredSize()", label.preferredSize)
  append("SwingUtilities.layoutCompoundLabel(...)", getLayoutCompoundLabelBounds().size)
}

private fun append(s: String, o: Any) {
  log.append("$s:\n  ")
  if (o is Rectangle2D) {
    log.append("x=%8.4f y=%8.4f w=%8.4f h=%8.4f%n".format(o.x, o.y, o.width, o.height))
  } else {
    log.append("$o")
  }
}

private fun getLayoutCompoundLabelBounds(): Rectangle {
  val viewR = Rectangle()
  val iconR = Rectangle()
  val textR = Rectangle()
  SwingUtilities.layoutCompoundLabel(
    label,
    label.getFontMetrics(label.font),
    label.text,
    null, // icon,
    label.verticalAlignment,
    label.horizontalAlignment,
    label.verticalTextPosition,
    label.horizontalTextPosition,
    viewR,
    iconR,
    textR,
    0
  )
  return textR
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
