package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI() = JPanel(BorderLayout()).also {
  val textArea = HighlightCursorTextArea()
  textArea.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  textArea.text = "MouseOver Painter Test\n\n**********************"
  val scroll = JScrollPane(textArea)
  scroll.viewport.background = Color.WHITE
  it.add(scroll)
  it.preferredSize = Dimension(320, 240)
}

private class HighlightCursorTextArea : JTextArea() {
  var rollOverRowIndex = -1
  private var rolloverHandler: MouseInputListener? = null

  override fun updateUI() {
    removeMouseMotionListener(rolloverHandler)
    removeMouseListener(rolloverHandler)
    super.updateUI()
    isOpaque = false
    background = Color(0x0, true) // Nimbus
    rolloverHandler = RollOverListener()
    addMouseMotionListener(rolloverHandler)
    addMouseListener(rolloverHandler)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val i = insets
    val h = g2.fontMetrics.height
    val y = rollOverRowIndex * h + i.top
    g2.paint = LINE_COLOR
    g2.fillRect(i.left, y, size.width - i.left - i.right, h)
    g2.dispose()
    super.paintComponent(g)
  }

  private inner class RollOverListener : MouseInputAdapter() {
    override fun mouseExited(e: MouseEvent) {
      rollOverRowIndex = -1
      e.component.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = getLineAtPoint(e.point)
      if (row != rollOverRowIndex) {
        rollOverRowIndex = row
        e.component.repaint()
      }
    }

    private fun getLineAtPoint(pt: Point): Int {
      val root = document.defaultRootElement
      return root.getElementIndex(viewToModel(pt))
      // Java 9: return root.getElementIndex(viewToModel2D(pt))
    }
  }

  companion object {
    private val LINE_COLOR = Color(0xFA_FA_DC)
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
