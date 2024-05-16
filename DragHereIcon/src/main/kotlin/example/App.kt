package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.io.File
import javax.swing.*

fun makeUI(): Component {
  val label = JLabel(DragHereIcon())
  label.text = "<html>Drag <b>Files</b> Here"
  label.verticalTextPosition = SwingConstants.BOTTOM
  label.horizontalTextPosition = SwingConstants.CENTER
  label.foreground = Color.GRAY
  label.font = Font(Font.SERIF, Font.PLAIN, 24)
  label.dropTarget = DropTarget(
    label,
    DnDConstants.ACTION_COPY,
    FileDropTargetAdapter(),
    true,
  )
  return JPanel().also {
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DragHereIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    g2.stroke = BasicStroke(BORDER_SIZE)
    g2.paint = LINE_COLOR
    g2.draw(BORDER)
    g2.stroke = BasicStroke(SLIT_WIDTH)
    g2.paint = UIManager.getColor("Panel.background")
    val n = SLIT_NUM + 1
    val v = ICON_WIDTH / n
    val m = n * v
    for (i in 1 until n) {
      val a = i * v
      g2.drawLine(a, 0, a, m)
      g2.drawLine(0, a, m, a)
    }
    val frc = g2.fontRenderContext
    val arrow = TextLayout("⇩", FONT, frc).getOutline(null)
    g2.paint = LINE_COLOR
    val b = arrow.bounds2D
    val cx = ICON_WIDTH / 2.0 - b.centerX
    val cy = ICON_HEIGHT / 2.0 - b.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(cx, cy)
    g2.fill(toCenterAt.createTransformedShape(arrow))
    g2.dispose()
  }

  override fun getIconWidth() = ICON_WIDTH

  override fun getIconHeight() = ICON_HEIGHT

  companion object {
    private const val ICON_WIDTH = 100
    private const val ICON_HEIGHT = 100
    private const val BORDER_SIZE = 8f
    private const val SLIT_WIDTH = 8f
    private const val ARC_SIZE = 16
    private const val SLIT_NUM = 3
    private val BORDER = RoundRectangle2D.Double(
      BORDER_SIZE.toDouble(),
      BORDER_SIZE.toDouble(),
      (ICON_WIDTH - 2 * BORDER_SIZE - 1).toDouble(),
      (ICON_HEIGHT - 2 * BORDER_SIZE - 1).toDouble(),
      ARC_SIZE.toDouble(),
      ARC_SIZE.toDouble(),
    )
    private val FONT = Font(Font.MONOSPACED, Font.BOLD, ICON_WIDTH)
    private val LINE_COLOR = Color.GRAY
  }
}

private class FileDropTargetAdapter : DropTargetAdapter() {
  override fun dragOver(e: DropTargetDragEvent) {
    if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      e.acceptDrag(DnDConstants.ACTION_COPY)
    } else {
      e.rejectDrag()
    }
  }

  override fun drop(e: DropTargetDropEvent) {
    runCatching {
      if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        e.acceptDrop(DnDConstants.ACTION_COPY)
        (e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.also {
          val msg = it.filter { o -> o is File }
            .map { o -> (o as? File)?.absolutePath + "<br>" }
            .fold("<html>") { o, s -> o + s }
          JOptionPane.showMessageDialog(null, msg)
          e.dropComplete(true)
        } ?: e.rejectDrop()
      } else {
        e.rejectDrop()
      }
    }.onFailure {
      e.rejectDrop()
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
