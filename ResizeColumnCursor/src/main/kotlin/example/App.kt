package example

import com.sun.java.swing.plaf.windows.WindowsTableHeaderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputListener
import javax.swing.plaf.basic.BasicSplitPaneUI
import javax.swing.plaf.basic.BasicTableHeaderUI
import javax.swing.plaf.synth.SynthTableHeaderUI
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn

fun makeUI(): Component {
  val sp = makeSplitPane(false)
  (sp.ui as? BasicSplitPaneUI)?.divider?.also {
    it.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
  }

  return JPanel(GridLayout(0, 1)).also {
    it.add(makeSplitPane(false))
    it.add(sp)
    it.add(makeSplitPane(true))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSplitPane(flag: Boolean): JSplitPane {
  val sp = if (flag) {
    object : JSplitPane(HORIZONTAL_SPLIT) {
      override fun updateUI() {
        super.updateUI()
        EventQueue.invokeLater {
          (getUI() as? BasicSplitPaneUI)?.divider?.also {
            it.cursor = ResizeCursorUtils.createCursor("⇹", 32, 32, it)
          }
        }
      }
    }
  } else {
    JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  }
  sp.isContinuousLayout = true
  sp.topComponent = JScrollPane(JTree())
  sp.bottomComponent = JScrollPane(if (flag) makeTable() else JTable(2, 3))
  EventQueue.invokeLater { sp.setDividerLocation(.3) }
  return sp
}

private fun makeTable() = object : JTable(2, 3) {
  override fun createDefaultTableHeader() = object : JTableHeader(columnModel) {
    override fun updateUI() {
      super.updateUI()
      val headerUI = when (getUI()) {
        is WindowsTableHeaderUI -> MyWindowsTableHeaderUI()
        is SynthTableHeaderUI -> MySynthTableHeaderUI()
        else -> MyBasicTableHeaderUI()
      }
      setUI(headerUI)
    }
  }
}

private object ResizeCursorUtils {
  fun createCursor(s: String?, width: Int, height: Int, c: Component): Cursor {
    val size = height - 2f
    val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    g2.font = c.font.deriveFont(size)
    val shape = TextLayout(s, g2.font, g2.fontRenderContext).getOutline(null)
    val icon = ShapeIcon(shape, width, height)
    icon.paintIcon(c, g2, 0, 0)
    g2.dispose()
    val hotSpot = Point(width / 2, height / 2)
    return c.toolkit.createCustomCursor(bi, hotSpot, s)
  }

  fun canResize(header: JTableHeader, p: Point): Boolean {
    val column = getResizeColumn(header, p)
    return column != null && header.resizingAllowed && column.resizable
  }

  private fun getResizeColumn(header: JTableHeader, p: Point): TableColumn? {
    val column = header.columnAtPoint(p)
    return if (column == -1) {
      null
    } else {
      val r = header.getHeaderRect(column).also {
        it.grow(-3, 0)
      }
      if (r.contains(p)) null else getResizeColumn(r, header, p, column)
    }
  }

  private fun getResizeColumn(
    rect: Rectangle,
    header: JTableHeader,
    pt: Point,
    column: Int
  ): TableColumn? {
    val midPoint = rect.x + rect.width / 2
    val columnIndex = if (header.componentOrientation.isLeftToRight) {
      if (pt.x < midPoint) column - 1 else column
    } else {
      if (pt.x < midPoint) column else column - 1
    }
    return if (columnIndex == -1) null else header.columnModel.getColumn(columnIndex)
  }
}

private class MyWindowsTableHeaderUI : WindowsTableHeaderUI() {
  override fun createMouseInputListener(): MouseInputListener {
    return object : MouseInputHandler() {
      private val resizeCursor = ResizeCursorUtils.createCursor("⇼", 32, 32, header)
      private val defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      override fun mouseMoved(e: MouseEvent) {
        super.mouseMoved(e)
        if (header.isEnabled) {
          header.cursor = if (ResizeCursorUtils.canResize(header, e.point)) {
            resizeCursor
          } else {
            defaultCursor
          }
        }
      }
    }
  }
}

private class MySynthTableHeaderUI : SynthTableHeaderUI() {
  override fun createMouseInputListener(): MouseInputListener {
    return object : MouseInputHandler() {
      private val resizeCursor = ResizeCursorUtils.createCursor("⇼", 32, 32, header)
      private val defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      override fun mouseMoved(e: MouseEvent) {
        super.mouseMoved(e)
        if (header.isEnabled) {
          header.cursor = if (ResizeCursorUtils.canResize(header, e.point)) {
            resizeCursor
          } else {
            defaultCursor
          }
        }
      }
    }
  }
}

private class MyBasicTableHeaderUI : BasicTableHeaderUI() {
  override fun createMouseInputListener(): MouseInputListener {
    return object : MouseInputHandler() {
      private val resizeCursor = ResizeCursorUtils.createCursor("⇼", 32, 32, header)
      private val defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      override fun mouseMoved(e: MouseEvent) {
        super.mouseMoved(e)
        if (header.isEnabled) {
          header.cursor = if (ResizeCursorUtils.canResize(header, e.point)) {
            resizeCursor
          } else {
            defaultCursor
          }
        }
      }
    }
  }
}

private class ShapeIcon(
  private val shape: Shape,
  private val width: Int,
  private val height: Int
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    val b = shape.bounds2D
    val p = Point2D.Double(b.x + b.width / 2.0, b.y + b.height / 2.0)
    val toCenterAt = AffineTransform.getTranslateInstance(width / 2.0 - p.x, height / 2.0 - p.y)
    g2.paint = c.foreground
    g2.fill(toCenterAt.createTransformedShape(shape))
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
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
