package example

import java.awt.*
import java.awt.geom.Rectangle2D
import javax.swing.*

fun makeUI(): Component {
  val layout = SpringLayout()
  val panel = JPanel(layout)
  panel.border = BorderFactory.createLineBorder(Color.GREEN, 10)
  val l1 = JLabel("label: 5%, 5%, 90%, 55%", SwingConstants.CENTER)
  l1.isOpaque = true
  l1.background = Color.ORANGE
  l1.border = BorderFactory.createLineBorder(Color.RED)
  val l2 = JButton("button: 50%, 65%, 40%, 30%")
  setScaleAndAdd(panel, layout, l1, Rectangle2D.Float(.05f, .05f, .90f, .55f))
  setScaleAndAdd(panel, layout, l2, Rectangle2D.Float(.50f, .65f, .40f, .30f))

  return JPanel(BorderLayout()).also {
    it.add(panel)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setScaleAndAdd(
  parent: Container,
  layout: SpringLayout,
  child: Component,
  r: Rectangle2D.Float
) {
  val pw = layout.getConstraint(SpringLayout.WIDTH, parent)
  val ph = layout.getConstraint(SpringLayout.HEIGHT, parent)
  val c = layout.getConstraints(child)
  c.x = Spring.scale(pw, r.x)
  c.y = Spring.scale(ph, r.y)
  c.width = Spring.scale(pw, r.width)
  c.height = Spring.scale(ph, r.height)
  parent.add(child)
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
