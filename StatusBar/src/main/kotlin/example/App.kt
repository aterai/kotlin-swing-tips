package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTextArea()))
  it.add(StatusBar(), BorderLayout.SOUTH)
  it.preferredSize = Dimension(320, 240)
}

private class StatusBar : JPanel(BorderLayout()) {
  init {
    add(BottomRightCornerLabel(), BorderLayout.EAST)
    isOpaque = false
  }
}

private class BottomRightCornerLabel : JLabel(BottomRightCornerIcon()) {
  @Transient private var handler: MouseInputListener? = null
  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    handler = ResizeWindowListener()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
  }
}

private class ResizeWindowListener : MouseInputAdapter() {
  private val rect = Rectangle()
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    val p = SwingUtilities.getRoot(e.component)
    if (p is Window) {
      startPt.location = e.point
      rect.bounds = p.getBounds()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val p = SwingUtilities.getRoot(e.component)
    if (!rect.isEmpty && p is Window) {
      val pt = e.point
      rect.width += pt.x - startPt.x
      rect.height += pt.y - startPt.y
      p.setBounds(rect)
    }
  }
}

private class BottomRightCornerIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val diff = 3
    val g2 = g.create() as Graphics2D
    g2.translate(iconWidth - diff * 3 - 1, iconHeight - diff * 3 - 1)
    val firstRow = 0
    val secondRow = firstRow + diff
    val thirdRow = secondRow + diff
    val firstColumn = 0
    drawSquare(g2, firstColumn, thirdRow)
    val secondColumn = firstColumn + diff
    drawSquare(g2, secondColumn, secondRow)
    drawSquare(g2, secondColumn, thirdRow)
    val thirdColumn = secondColumn + diff
    drawSquare(g2, thirdColumn, firstRow)
    drawSquare(g2, thirdColumn, secondRow)
    drawSquare(g2, thirdColumn, thirdRow)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 20

  private fun drawSquare(g: Graphics, x: Int, y: Int) {
    g.color = SQUARE_COLOR
    g.fillRect(x, y, 2, 2)
  }

  companion object {
    private val SQUARE_COLOR = Color(160, 160, 160, 160)
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
