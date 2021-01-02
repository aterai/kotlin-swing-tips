package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports

private var mode = Crossfade.IN

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val check = JCheckBox("Crossfade Type?", true)
  val icon1 = ImageIcon(cl.getResource("example/test.png"))
  val icon2 = ImageIcon(cl.getResource("example/test.jpg"))
  val button = JButton("change")
  val alpha = AtomicInteger(10)
  val crossfade = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = background
      g2.fillRect(0, 0, width, height)
      if (check.isSelected) {
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alpha.get() * .1f)
      }
      g2.drawImage(icon1.image, 0, 0, icon1.iconWidth, icon1.iconHeight, this)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.get() * .1f)
      g2.drawImage(icon2.image, 0, 0, icon2.iconWidth, icon2.iconHeight, this)
      g2.dispose()
    }
  }

  val animator = Timer(50, null)
  animator.addActionListener {
    when {
      mode == Crossfade.IN && alpha.get() < 10 -> alpha.incrementAndGet()
      mode == Crossfade.OUT && alpha.get() > 0 -> alpha.decrementAndGet()
      else -> animator.stop()
    }
    crossfade.repaint()
  }

  button.addActionListener {
    mode = mode.toggle()
    animator.start()
  }

  return JPanel(BorderLayout()).also {
    it.add(crossfade)
    it.add(button, BorderLayout.NORTH)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class Crossfade {
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
