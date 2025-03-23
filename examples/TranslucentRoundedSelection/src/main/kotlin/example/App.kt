package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import javax.swing.*
import javax.swing.plaf.TextUI
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
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

fun makeUI(): Component {
  val textArea = object : JTextArea(TEXT) {
    override fun updateUI() {
      super.updateUI()
      val caret = RoundedSelectionCaret()
      caret.blinkRate = UIManager.getInt("TextArea.caretBlinkRate")
      setCaret(caret)
      (highlighter as? DefaultHighlighter)?.drawsLayeredHighlights = false
      selectedTextColor = null
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
  tabs.add("JEditorPane", JScrollPane(makeEditorPane()))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeEditorPane(): JEditorPane {
  val editor: JEditorPane = object : JEditorPane() {
    override fun updateUI() {
      super.updateUI()
      val caret = RoundedSelectionCaret()
      caret.blinkRate = UIManager.getInt("TextArea.caretBlinkRate")
      setCaret(caret)
      (highlighter as? DefaultHighlighter)?.drawsLayeredHighlights = false
    }
  }
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = makeStyleSheet()
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

private fun makeStyleSheet(): StyleSheet {
  val styleSheet = StyleSheet()
  styleSheet.addRule(".str {color:#008800}")
  styleSheet.addRule(".kwd {color:#000088}")
  styleSheet.addRule(".com {color:#880000}")
  styleSheet.addRule(".typ {color:#660066}")
  styleSheet.addRule(".lit {color:#006666}")
  styleSheet.addRule(".pun {color:#666600}")
  styleSheet.addRule(".pln {color:#000000}")
  styleSheet.addRule(".tag {color:#000088}")
  styleSheet.addRule(".atn {color:#660066}")
  styleSheet.addRule(".atv {color:#008800}")
  styleSheet.addRule(".dec {color:#660066}")
  return styleSheet
}

private class RoundedSelectionCaret : DefaultCaret() {
  override fun getSelectionPainter() = RoundedSelectionHighlightPainter()

  @Synchronized
  override fun damage(r: Rectangle) {
    val c = component
    val startOffset = c.selectionStart
    val endOffset = c.selectionEnd
    if (startOffset == endOffset) {
      super.damage(r)
    } else {
      val mapper = c.ui
      kotlin
        .runCatching {
          val p0 = mapper.modelToView(c, startOffset)
          val p1 = mapper.modelToView(c, endOffset)
          val h = (p1.maxY - p0.minY).toInt()
          c.repaint(Rectangle(0, p0.y, c.width, h))
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(c)
        }
    }
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
    val color = c.selectionColor
    g2.color = Color(color.red, color.green, color.blue, 64)
    runCatching {
      val area: Area = getLinesArea(c, offs0, offs1)
      for (a in GeomUtils.singularization(area)) {
        val lst = GeomUtils.convertAreaToPoint2DList(a)
        GeomUtils.flatteningStepsOnRightSide(lst, 3.0 * 2.0)
        g2.fill(GeomUtils.convertRoundedPath(lst, 3.0))
      }
    }
    g2.dispose()
  }

  @Throws(BadLocationException::class)
  private fun getLinesArea(c: JTextComponent, offs0: Int, offs1: Int): Area {
    val mapper: TextUI = c.ui
    val area = Area()
    var cur = offs0
    do {
      val startOffset: Int = Utilities.getRowStart(c, cur)
      val endOffset: Int = Utilities.getRowEnd(c, cur)
      val p0: Rectangle = mapper.modelToView(c, max(startOffset, offs0))
      val p1: Rectangle = mapper.modelToView(c, min(endOffset, offs1))
      if (p0.x == p1.x) {
        p0.width += 6
        area.add(Area(p0))
      } else {
        area.add(Area(p0.union(p1)))
      }
      cur = endOffset + 1
    } while (cur < offs1)
    return area
  }
}

private object GeomUtils {
  fun convertAreaToPoint2DList(area: Area): MutableList<Point2D> {
    val list = mutableListOf<Point2D>()
    val pi = area.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      val pathSegmentType = pi.currentSegment(coords)
      when (pathSegmentType) {
        PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO ->
          list.add(Point2D.Double(coords[0], coords[1]))
      }
      pi.next()
    }
    return list
  }

  fun flatteningStepsOnRightSide(
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
    val kappa = 4.0 * (sqrt(2.0) - 1.0) / 3.0
    val akv = arc - arc * kappa
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

  fun singularization(rect: Area): List<Area> {
    val list = mutableListOf<Area>()
    val path = Path2D.Double()
    val pi = rect.getPathIterator(null)
    val coords = DoubleArray(6)
    while (!pi.isDone) {
      val pathSegmentType = pi.currentSegment(coords)
      when (pathSegmentType) {
        PathIterator.SEG_MOVETO -> path.moveTo(
          coords[0],
          coords[1],
        )

        PathIterator.SEG_LINETO -> path.lineTo(
          coords[0],
          coords[1],
        )

        PathIterator.SEG_QUADTO -> path.quadTo(
          coords[0],
          coords[1],
          coords[2],
          coords[3],
        )

        PathIterator.SEG_CUBICTO -> path.curveTo(
          coords[0],
          coords[1],
          coords[2],
          coords[3],
          coords[4],
          coords[5],
        )

        PathIterator.SEG_CLOSE -> path.also {
          it.closePath()
          list.add(Area(it))
          it.reset()
        }
      }
      pi.next()
    }
    return list
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
