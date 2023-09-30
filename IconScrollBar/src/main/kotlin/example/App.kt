package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

fun makeUI(): Component {
  val l = object : JLabel("1234567890") {
    override fun getPreferredSize() = Dimension(1000, 1000)
  }
  val scrollPane = JScrollPane(l)
  if (scrollPane.verticalScrollBar.ui is WindowsScrollBarUI) {
    scrollPane.verticalScrollBar.ui = BasicIconScrollBarUI()
  } else {
    scrollPane.verticalScrollBar.ui = WindowsIconScrollBarUI()
  }
  return JPanel(BorderLayout()).also {
    it.add(scrollPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class WindowsIconScrollBarUI : WindowsScrollBarUI() {
  override fun paintThumb(
    g: Graphics,
    c: JComponent,
    thumbBounds: Rectangle,
  ) {
    super.paintThumb(g, c, thumbBounds)
    val sb = c as? JScrollBar
    if (sb?.isEnabled != true || thumbBounds.width > thumbBounds.height) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val oc: Color
    val ic: Color
    when {
      isDragging -> {
        oc = SystemColor.activeCaption.darker()
        ic = SystemColor.inactiveCaptionText.darker()
      }
      isThumbRollover -> {
        oc = SystemColor.activeCaption.brighter()
        ic = SystemColor.inactiveCaptionText.brighter()
      }
      else -> {
        oc = SystemColor.activeCaption
        ic = SystemColor.inactiveCaptionText
      }
    }
    paintCircle(g2, thumbBounds, 6, oc)
    paintCircle(g2, thumbBounds, 10, ic)
    g2.dispose()
  }

  private fun paintCircle(
    g2: Graphics2D,
    thumbBounds: Rectangle,
    w: Int,
    color: Color,
  ) {
    g2.paint = color
    val ww = thumbBounds.width - w
    g2.fillOval(thumbBounds.x + w / 2, thumbBounds.y + (thumbBounds.height - ww) / 2, ww, ww)
  }
}

private class BasicIconScrollBarUI : BasicScrollBarUI() {
  override fun paintThumb(
    g: Graphics,
    c: JComponent,
    thumbBounds: Rectangle,
  ) {
    super.paintThumb(g, c, thumbBounds)
    val sb = c as? JScrollBar
    if (sb?.isEnabled != true || thumbBounds.width > thumbBounds.height) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val oc: Color
    val ic: Color
    when {
      isDragging -> {
        oc = SystemColor.activeCaption.darker()
        ic = SystemColor.inactiveCaptionText.darker()
      }
      isThumbRollover -> {
        oc = SystemColor.activeCaption.brighter()
        ic = SystemColor.inactiveCaptionText.brighter()
      }
      else -> {
        oc = SystemColor.activeCaption
        ic = SystemColor.inactiveCaptionText
      }
    }
    paintCircle(g2, thumbBounds, 6, oc)
    paintCircle(g2, thumbBounds, 10, ic)
    g2.dispose()
  }

  private fun paintCircle(
    g2: Graphics2D,
    thumbBounds: Rectangle,
    w: Int,
    color: Color,
  ) {
    g2.paint = color
    val ww = thumbBounds.width - w
    g2.fillOval(thumbBounds.x + w / 2, thumbBounds.y + (thumbBounds.height - ww) / 2, ww, ww)
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
