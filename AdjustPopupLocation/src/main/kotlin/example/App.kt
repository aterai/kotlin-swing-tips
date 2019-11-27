package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val check = JCheckBox("Adjust JPopupMenu location", true)
  // check.setFocusPainted(false)
  val popup: JPopupMenu = object : JPopupMenu() {
    override fun show(c: Component, x: Int, y: Int) {
      if (check.isSelected()) {
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
  label.setOpaque(true)
  label.setComponentPopupMenu(popup)
  val p = JPanel(BorderLayout())
  p.add(check, BorderLayout.NORTH)
  p.add(label)
  p.setBorder(BorderFactory.createLineBorder(Color.RED, 10))
  p.setPreferredSize(Dimension(320, 240))
  return p
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
