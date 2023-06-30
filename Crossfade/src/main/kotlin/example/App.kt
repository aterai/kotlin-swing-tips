package example

import java.awt.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.Timer

private var mode = CrossFade.IN

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val check = JCheckBox("CrossFade Type?", true)
  val icon1 = ImageIcon(requireNotNull(cl.getResource("example/test.png")))
  val icon2 = ImageIcon(requireNotNull(cl.getResource("example/test.jpg")))
  val button = JButton("change")
  val alpha = AtomicInteger(10)
  val fade = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = background
      g2.fillRect(0, 0, width, height)
      if (check.isSelected) {
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alpha.get() * .1f)
      }
      icon1.paintIcon(this, g2, 0, 0)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.get() * .1f)
      icon2.paintIcon(this, g2, 0, 0)
      g2.dispose()
    }
  }

  val animator = Timer(50, null)
  animator.addActionListener {
    when {
      mode == CrossFade.IN && alpha.get() < 10 -> alpha.incrementAndGet()
      mode == CrossFade.OUT && alpha.get() > 0 -> alpha.decrementAndGet()
      else -> animator.stop()
    }
    fade.repaint()
  }

  button.addActionListener {
    mode = mode.toggle()
    animator.start()
  }

  return JPanel(BorderLayout()).also {
    it.add(fade)
    it.add(button, BorderLayout.NORTH)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class CrossFade {
  IN, OUT;

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
