package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/16x16.png"))
  val p = object : JPanel(BorderLayout()) {
    override fun paintComponent(g: Graphics) {
      val d = size
      val w = icon.iconWidth
      val h = icon.iconHeight
      val image = icon.image
      var i = 0
      while (i * w < d.width) {
        var j = 0
        while (j * h < d.height) {
          g.drawImage(image, i * w, j * h, w, h, this)
          j++
        }
        i++
      }
      super.paintComponent(g)
    }
  }
  p.add(JLabel("BackgroundImage"))
  p.isOpaque = false
  p.preferredSize = Dimension(320, 240)
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
