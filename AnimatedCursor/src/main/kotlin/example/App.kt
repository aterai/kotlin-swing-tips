package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val pt = Point()
  val tk = Toolkit.getDefaultToolkit()
  val list = listOf("00", "01", "02").map {
    tk.createCustomCursor(makeImage(it), pt, it)
  }

  val animator = Timer(100, null)
  val button = JButton("Start")
  button.cursor = list[0]
  button.addActionListener { e ->
    (e.source as? JButton)?.also {
      if (animator.isRunning) {
        it.text = "Start"
        animator.stop()
      } else {
        it.text = "Stop"
        animator.start()
      }
    }
  }
  button.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      animator.stop()
    }
  }
  animator.addActionListener(CursorActionListener(button, list))

  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createEmptyBorder(32, 32, 32, 32)
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createTitledBorder("delay=100ms")
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeImage(name: String): Image {
  val path = "example/$name.pmg"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (32 - iw) / 2, (32 - ih) / 2)
  g2.dispose()
  return bi
}

private class CursorActionListener(
  private val comp: Component,
  private val frames: List<Cursor>
) : ActionListener {
  private var counter = 0

  override fun actionPerformed(e: ActionEvent) {
    comp.cursor = frames[counter]
    counter = (counter + 1) % frames.size
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
