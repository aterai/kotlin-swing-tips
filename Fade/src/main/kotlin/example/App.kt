package example

import java.awt.*
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.swing.*

private var mode = Fade.IN

fun makeUI(): Component {
  val animator = Timer(25, null)
  val alpha = AtomicInteger(10)

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
    ?: UIManager.getIcon("OptionPane.errorIcon")

  val fade = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color.BLACK
      g2.fillRect(0, 0, width, height)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.get() * .1f)
      icon.paintIcon(this, g2, 0, 0)
      g2.dispose()
    }
  }
  animator.addActionListener {
    if (mode == Fade.IN && alpha.get() < 10) {
      alpha.incrementAndGet()
    } else if (mode == Fade.OUT && alpha.get() > 0) {
      alpha.decrementAndGet()
    } else {
      animator.stop()
    }
    fade.repaint()
  }

  val button = JButton("Fade In/Out")
  button.addActionListener {
    mode = mode.toggle()
    animator.start()
  }

  return JPanel(BorderLayout()).also {
    it.add(fade)
    it.add(button, BorderLayout.SOUTH)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class Fade {
  IN,
  OUT;

  fun toggle() = if (this == IN) OUT else IN
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
