package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

fun makeUI(): Component {
  val box = Box.createVerticalBox().also {
    it.add(makeLabel("00000000000"))
    it.add(makeLabel("111111111111111111111111111"))
    it.add(makeLabel("1235436873434325"))
    it.add(makeLabel("22222222"))
    it.add(makeLabel("3333333333333333333333333333333333333333333"))
    it.add(makeLabel("1235436873434325"))
    it.add(Box.createVerticalGlue())
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(box))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabel(str: String) = JLabel(str).also {
  it.border = RippleBorder(it, 10)
}

private class RippleBorder(c: Component, size: Int) : EmptyBorder(size, size, size, size) {
  private val animator: Timer
  private var count = 1f

  init {
    animator = Timer(80) {
      c.repaint()
      count += .9f
    }
    c.addMouseListener(object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) {
        e.component.foreground = Color.RED
        animator.start()
      }

      override fun mouseExited(e: MouseEvent) {
        e.component.foreground = Color.BLACK
      }
    })
  }

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
    if (!animator.isRunning) {
      super.paintBorder(c, g, x, y, w, h)
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color.WHITE
    var a = 1f / count
    val shouldBeHidden = .12f - a > 1.0e-2
    if (shouldBeHidden) {
      a = 0f
    }
    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a)
    val i = borderInsets
    val xx = i.left - count.toInt()
    val yy = i.top - count.toInt()
    val ww = i.left + i.right - (count * 2f).toInt()
    val hh = i.top + i.bottom - (count * 2f).toInt()
    g2.stroke = BasicStroke(count * 1.2f)
    g2.drawRoundRect(xx, yy, w - ww, h - hh, 10, 10)
    if (xx < 0 && animator.isRunning) {
      count = 1f
      animator.stop()
    }
    g2.dispose()
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
