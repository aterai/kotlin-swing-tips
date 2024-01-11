package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.metal.MetalScrollBarUI

private const val LF = "\n"

fun makeUI(): Component {
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(32, 32))
  val buf = StringBuilder()
  for (i in 0 until 1000) {
    buf.append(i).append(LF)
  }
  val txt = buf.toString()

  val scroll = JScrollPane(JTextArea("override\ngetMinimumThumbSize()\n$txt"))
  scroll.verticalScrollBar = object : JScrollBar(VERTICAL) {
    override fun updateUI() {
      super.updateUI()
      val barUI = if (ui is WindowsScrollBarUI) {
        object : WindowsScrollBarUI() {
          override fun getMinimumThumbSize(): Dimension {
            val d = super.getMinimumThumbSize()
            val r = SwingUtilities.calculateInnerArea(scroll, null)
            return Dimension(d.width, d.height.coerceAtLeast(r.height / 12))
          }
        }
      } else {
        object : MetalScrollBarUI() {
          override fun getMinimumThumbSize(): Dimension {
            val d = super.getMinimumThumbSize()
            val r = SwingUtilities.calculateInnerArea(scroll, null)
            d.height = d.height.coerceAtLeast(r.height / 12)
            return d
          }
        }
      }
      setUI(barUI)
      putClientProperty("JScrollBar.fastWheelScrolling", true)
    }
  }
  val s0 = JScrollPane(JTextArea("default\n\n$txt"))
  return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, s0, scroll).also {
    it.resizeWeight = .5
    it.dividerLocation = 160
    it.preferredSize = Dimension(320, 240)
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
