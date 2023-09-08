package example

import java.awt.*
import javax.swing.*

private var wipeMode = Wipe.IN

fun makeUI(): Component {
  val animator = Timer(5, null)
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/test.png"))
  val wipe = object : JComponent() {
    private var ww = 0

    override fun paintComponent(g: Graphics) {
      g.color = background
      g.fillRect(0, 0, width, height)
      if (wipeMode == Wipe.IN) {
        if (ww < icon.iconWidth) {
          ww += 10
        } else {
          animator.stop()
        }
      } else { // Wipe.OUT:
        if (ww > 0) {
          ww -= 10
        } else {
          animator.stop()
        }
      }
      val iw = icon.iconWidth
      val ih = icon.iconHeight
      g.drawImage(icon.image, 0, 0, iw, ih, this)
      g.fillRect(ww, 0, iw, ih)
    }
  }
  wipe.background = Color.BLACK
  animator.addActionListener { wipe.repaint() }

  val button1 = JButton("Wipe In")
  button1.addActionListener {
    wipeMode = Wipe.IN
    animator.start()
  }

  val button2 = JButton("Wipe Out")
  button2.addActionListener {
    wipeMode = Wipe.OUT
    animator.start()
  }

  return JPanel(BorderLayout()).also {
    it.add(wipe)
    it.add(button1, BorderLayout.SOUTH)
    it.add(button2, BorderLayout.NORTH)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
    animator.start()
  }
}

private enum class Wipe {
  IN,
  OUT,
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
