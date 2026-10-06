package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import javax.swing.text.Utilities
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

private const val TEXT = """
Trail: Creating a GUI with JFC/Swing
Lesson: Learning Swing by Example
This lesson explains the concepts you need to
  use Swing components in building a user interface.
  First we examine the simplest Swing application you can write.
  Then we present several progressively complicated examples of creating
  user interfaces using components in the javax.swing package.
  We cover several Swing components, such as buttons, labels, and text areas.
  The handling of events is also discussed,
  as are layout management and accessibility.
  This lesson ends with a set of questions and exercises
  so you can test yourself on what you've learned.
https://docs.oracle.com/javase/tutorial/uiswing/learn/index.html
"""

fun createUI(): Component {
  val textArea = object : JTextArea(TEXT) {
    override fun updateUI() {
      super.updateUI()
      // PlainView ignores setSelectedTextColor(null) and keeps the current color
      // of the Graphics (e.g. the background color), so use the foreground color
      selectedTextColor = foreground
      installRoundedSelection(this)
    }
  }
  val check = JCheckBox("setLineWrap / setWrapStyleWord:")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    textArea.lineWrap = b
    textArea.wrapStyleWord = b
  }
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(Box.createHorizontalGlue())
  box.add(check)

  val p = JPanel(BorderLayout())
  p.add(JScrollPane(textArea))
  p.add(box, BorderLayout.SOUTH)

  val tabs = JTabbedPane()
  tabs.addTab("JTextArea", p)
  tabs.add("JEditorPane", JScrollPane(createEditorPane()))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun installRoundedSelection(c: JTextComponent) {
  val caret = RoundedSelectionCaret()
  caret.blinkRate = c.caret.blinkRate
  c.caret = caret
  (c.highlighter as? DefaultHighlighter)?.drawsLayeredHighlights = false
}

private fun createEditorPane(): JEditorPane {
  val editor = object : JEditorPane() {
    override fun updateUI() {
      super.updateUI()
      // GlyphView does not change the text color if the selected text color is null
      selectedTextColor = null
      installRoundedSelection(this)
    }
  }
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = createStyleSheet()
  editor.editorKit = htmlEditorKit
  editor.isEditable = false
  editor.background = Color(0xEE_EE_EE)
  val cl = Thread.currentThread().contextClassLoader
  cl.getResource("example/test.html")?.also { url ->
    runCatching {
      editor.page = url
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(editor)
      editor.text = it.message
    }
  }
  return editor
}

private fun createStyleSheet(): StyleSheet {
  val styleSheet = StyleSheet()
  styleSheet.addRule(".str{color:#008800}")
  styleSheet.addRule(".kwd{color:#000088}")
  styleSheet.addRule(".com{color:#880000}")
  styleSheet.addRule(".typ{color:#660066}")
  styleSheet.addRule(".lit{color:#006666}")
  styleSheet.addRule(".pun{color:#666600}")
  styleSheet.addRule(".pln{color:#000000}")
  styleSheet.addRule(".tag{color:#000088}")
  styleSheet.addRule(".atn{color:#660066}")
  styleSheet.addRule(".atv{color:#008800}")
  styleSheet.addRule(".dec{color:#660066}")
  return styleSheet
}

private class RoundedSelectionCaret : DefaultCaret() {
  override fun getSelectionPainter(): Highlighter.HighlightPainter = PAINTER

  // The default damage area does not cover the rounded corners on the right side,
  // so repaint the full width of the rows from the selection start to the end.
  @Synchronized
  override fun damage(r: Rectangle) {
    super.damage(r)
    val c = component
    val mapper = c.ui
    runCatching {
      // Java 9: mapper.modelToView2D(c, offs, Position.Bias.Forward).getBounds()
      val p0: Rectangle? = mapper.modelToView(c, c.selectionStart)
      val p1: Rectangle? = mapper.modelToView(c, c.selectionEnd)
      if (p0 != null && p1 != null) {
        val rect = p0.union(p1)
        c.repaint(0, rect.y, c.width, rect.height)
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(c)
    }
  }

  companion object {
    private val PAINTER = RoundedSelectionHighlightPainter()
  }
}

private class RoundedSelectionHighlightPainter : DefaultHighlightPainter(null) {
  override fun paint(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val rgb = c.selectionColor.rgb and 0xFF_FF_FF
    g2.color = Color(ALPHA shl 24 or rgb, true)
    runCatching {
      val area = getRowsArea(c, offs0, offs1)
      for (polygon in GeomUtils.splitIntoPolygons(area)) {
        GeomUtils.snapShortRightEdges(polygon, ARC * 2.0)
        g2.fill(GeomUtils.convertRoundedPath(polygon, ARC.toDouble()))
      }
    }
    g2.dispose()
  }

  // Union of the selected text bounds of each row (not the full width of the rows).
  @Throws(BadLocationException::class)
  private fun getRowsArea(c: JTextComponent, offs0: Int, offs1: Int): Area {
    val mapper = c.ui
    val area = Area()
    var cur = offs0
    do {
      val rowStart = Utilities.getRowStart(c, cur)
      val rowEnd = Utilities.getRowEnd(c, cur)
      if (rowStart < 0 || rowEnd < 0) {
        break
      }
      val p0: Rectangle? = mapper.modelToView(c, max(rowStart, offs0))
      val p1: Rectangle? = mapper.modelToView(c, min(rowEnd, offs1))
      if (p0 == null || p1 == null) {
        break
      }
      if (offs1 > rowEnd) {
        // The line break is selected: extend the row by the arc diameter
        p1.width += ARC * 2
      }
      area.add(Area(p0.union(p1)))
      cur = rowEnd + 1
    } while (cur < offs1)
    return area
  }

  companion object {
    const val ARC = 3
    private const val ALPHA = 64
  }
}

private object GeomUtils {
  private val KAPPA = 4.0 * (sqrt(2.0) - 1.0) / 3.0 // = 0.55228...

  // Decompose a multi-loop Area into a list of polygons (single-loop vertex lists).
  fun splitIntoPolygons(area: Area): List<MutableList<Point2D>> {
    val polygons = mutableListOf<MutableList<Point2D>>()
    var polygon = mutableListOf<Point2D>()
    val pi = area.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      when (pi.currentSegment(coords)) {
        PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> {
          polygon.add(Point2D.Double(coords[0], coords[1]))
        }

        PathIterator.SEG_CLOSE -> if (polygon.isNotEmpty()) {
          polygons.add(polygon)
          polygon = mutableListOf()
        }
      }
      pi.next()
    }
    return polygons
  }

  fun snapShortRightEdges(
    list: MutableList<Point2D>,
    arc: Double,
  ): List<Point2D> {
    val sz = list.size
    for (i in 0..<sz) {
      val i1 = (i + 1) % sz
      val i2 = (i + 2) % sz
      val i3 = (i + 3) % sz
      val pt0 = list[i]
      val pt1 = list[i1]
      val pt2 = list[i2]
      val pt3 = list[i3]
      val dx1 = pt2.x - pt1.x
      if (abs(dx1) > 1.0e-1 && abs(dx1) < arc) {
        val max = max(pt0.x, pt2.x)
        replace(list, i, max, pt0.y)
        replace(list, i1, max, pt1.y)
        replace(list, i2, max, pt2.y)
        replace(list, i3, max, pt3.y)
      }
    }
    return list
  }

  private fun replace(list: MutableList<Point2D>, i: Int, x: Double, y: Double) {
    list.removeAt(i)
    list.add(i, Point2D.Double(x, y))
  }

  fun convertRoundedPath(list: List<Point2D>, arc: Double): Path2D {
    val akv = arc - arc * KAPPA
    val pt0 = list[0]
    val path = Path2D.Double()
    val sz = list.size
    path.moveTo(pt0.x + arc, pt0.y)
    for (i in 0..<sz) {
      val prv = list[(i - 1 + sz) % sz]
      val cur = list[i]
      val nxt = list[(i + 1) % sz]
      val dx0 = sign(cur.x - prv.x)
      val dy0 = sign(cur.y - prv.y)
      val dx1 = sign(nxt.x - cur.x)
      val dy1 = sign(nxt.y - cur.y)
      path.curveTo(
        cur.x - dx0 * akv,
        cur.y - dy0 * akv,
        cur.x + dx1 * akv,
        cur.y + dy1 * akv,
        cur.x + dx1 * arc,
        cur.y + dy1 * arc,
      )
      path.lineTo(nxt.x - dx1 * arc, nxt.y - dy1 * arc)
    }
    path.closePath()
    return path
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
