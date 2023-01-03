package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val check = JCheckBox("Adjust JPopupMenu location", true)
  val popup = object : JPopupMenu() {
    override fun show(c: Component?, x: Int, y: Int) {
      if (check.isSelected && c != null) {
        val p = Point(x, y)
        val r = c.bounds
        val d = preferredSize
        if (p.x + d.width > r.width) {
          p.x -= d.width
        }
        if (p.y + d.height > r.height) {
          p.y -= d.height
        }
        super.show(c, p.x.coerceAtLeast(0), p.y.coerceAtLeast(0))
      } else {
        super.show(c, x, y)
      }
    }
  }
  popup.add("111")
  popup.add("222222")
  popup.add("33")
  val label = JLabel("JLabel")
  label.isOpaque = true
  label.componentPopupMenu = popup

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(label)
    it.border = BorderFactory.createLineBorder(Color.RED, 10)
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
