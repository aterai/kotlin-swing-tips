package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.io.Serializable
import javax.swing.* // ktlint-disable no-wildcard-imports

private val panelDim = Dimension(320, 240)
private val racket = Racket(panelDim)
private val absolute = JLabel("absolute:")
private val relative = JLabel("relative:")

fun makeUI(): Component {
  val p = object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = panelDim

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      racket.draw(g)
    }
  }

  val timer = Timer(10) {
    val pi = MouseInfo.getPointerInfo()
    val pt = pi.location
    absolute.text = "absolute:$pt"
    SwingUtilities.convertPointFromScreen(pt, p)
    relative.text = "relative:$pt"
    racket.move(pt.x)
    p.repaint()
  }

  p.addHierarchyListener { e ->
    if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) {
      if (e.component.isDisplayable) {
        timer.start()
      } else {
        timer.stop()
      }
    }
  }

  val box = Box.createVerticalBox()
  box.add(absolute)
  box.add(relative)
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(box)
  return p
}

private class Racket(private val parentSize: Dimension) : Serializable {
  private var centerPos: Int

  fun draw(g: Graphics) {
    g.color = Color.RED
    g.fillRect(
      centerPos - WIDTH / 2,
      parentSize.height - HEIGHT,
      WIDTH,
      HEIGHT
    )
  }

  fun move(pos: Int) {
    centerPos = pos
    if (centerPos < WIDTH / 2) {
      centerPos = WIDTH / 2
    } else if (centerPos > parentSize.width - WIDTH / 2) {
      centerPos = parentSize.width - WIDTH / 2
    }
  }

  companion object {
    private const val serialVersionUID = 1L
    private const val WIDTH = 80
    private const val HEIGHT = 5
  }

  init {
    centerPos = parentSize.width / 2
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
