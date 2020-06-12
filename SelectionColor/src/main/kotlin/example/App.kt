package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ByteLookupTable
import java.awt.image.LookupOp
import java.net.URL
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter.HighlightPainter
import javax.swing.text.JTextComponent
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

private val SELECTION = Color(0xC8_64_64_FF.toInt(), true)
private val HIGHLIGHT = Color(0x64_FF_FF_32, true)
private val editorPane = JEditorPane()

private fun makeUI(): Component {
  val check = JCheckBox("setSelectionColor(#C86464FF)", true)
  check.addActionListener { e ->
    val sc = if ((e.source as? JCheckBox)?.isSelected == true) SELECTION else null
    editorPane.selectionColor = sc
  }
  val styleSheet = StyleSheet()
  styleSheet.addRule(".highlight {color: blue; background: #FF5533; opacity: 0.5;}")
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet
  editorPane.editorKit = htmlEditorKit
  editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, java.lang.Boolean.TRUE)
  editorPane.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
  editorPane.isOpaque = false
  editorPane.foreground = Color(0xC8_C8_C8)
  editorPane.selectedTextColor = Color.WHITE
  editorPane.background = Color(0x0, true) // Nimbus
  editorPane.selectionColor = SELECTION
  editorPane.text =
    """
    <html><pre>
private static void createAndShowGui() {
  <span class='highlight'>JFrame</span> frame = new JFrame();
  frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
  frame.getContentPane().add(new MainPanel());
  frame.pack();
  frame.setLocationRelativeTo(null);
  frame.setVisible(true);
}
    """

  val highlightPainter: HighlightPainter = DefaultHighlightPainter(HIGHLIGHT)
  val button = JToggleButton("highlight")
  button.addActionListener { e ->
    if ((e.source as? JToggleButton)?.isSelected == true) {
      setHighlight(editorPane, "[Ff]rame", highlightPainter)
    } else {
      editorPane.highlighter.removeAllHighlights()
    }
  }
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/tokeidai.jpg")
  val bi = getFilteredImage(url)
  val scroll = JScrollPane(editorPane)
  scroll.viewport.isOpaque = false
  scroll.viewportBorder = CentredBackgroundBorder(bi)
  scroll.verticalScrollBar.unitIncrement = 25

  val box = Box.createHorizontalBox()
  box.add(check)
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(2))

  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.SOUTH)
  p.add(scroll)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun getFilteredImage(url: URL?): BufferedImage {
  val src = runCatching { ImageIO.read(url) }.getOrNull() ?: makeMissingImage()
  val dst = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
  val b = ByteArray(256)
  for (i in b.indices) {
    b[i] = (i * .2f).toByte()
  }
  LookupOp(ByteLookupTable(0, b), null).filter(src, dst)
  return dst
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

fun setHighlight(jtc: JTextComponent, pattern: String, painter: HighlightPainter) {
  val highlighter = jtc.highlighter
  highlighter.removeAllHighlights()
  val doc = jtc.document
  runCatching {
    val text = doc.getText(0, doc.length)
    val matcher = Pattern.compile(pattern).matcher(text)
    var pos = 0
    while (matcher.find(pos) && matcher.group().isNotEmpty()) {
      val start = matcher.start()
      val end = matcher.end()
      highlighter.addHighlight(start, end, painter)
      pos = end
    }
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(jtc)
  }
  jtc.repaint()
}

private class CentredBackgroundBorder(private val image: BufferedImage) : Border {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    val cx = (width - image.width) / 2.0
    val cy = (height - image.height) / 2.0
    g2.drawRenderedImage(image, AffineTransform.getTranslateInstance(cx, cy))
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(0, 0, 0, 0)

  override fun isBorderOpaque() = true
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
