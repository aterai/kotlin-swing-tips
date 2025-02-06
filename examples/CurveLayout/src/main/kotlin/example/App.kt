package example

import java.awt.*
import javax.swing.*
import kotlin.math.pow
import kotlin.math.sqrt

fun makeUI(): Component {
  val a2 = 4.0
  val panel1 = JPanel(FlowLayout(FlowLayout.LEFT))
  val panel2 = object : JPanel() {
    // protected val A2 = 4.0
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      val r = SwingUtilities.calculateInnerArea(this, null)
      g2.translate(r.x, r.y)
      g2.paint = Color.RED
      var px = 0
      var py = 0
      for (x in 0..<r.width) {
        val y = (x / a2).pow(2.0).toInt()
        g2.drawLine(px, py, x, y)
        px = x
        py = y
      }
      g2.dispose()
    }

    override fun updateUI() {
      super.updateUI()
      layout = object : FlowLayout() {
        override fun layoutContainer(target: Container) {
          synchronized(target.treeLock) {
            val num = target.componentCount
            if (num <= 0) {
              return
            }
            val r = SwingUtilities.calculateInnerArea(target as? JComponent, null)
            val rh = (r.height - vgap * 2) / num
            var x = r.x + hgap
            var y = r.y + vgap
            target.components.filter { it.isVisible }.forEach {
              val d = it.preferredSize
              it.setSize(d.width, d.height)
              it.setLocation(x, y)
              y += vgap + minOf(rh, d.height)
              x = (a2 * sqrt(y.toDouble())).toInt()
            }
          }
        }
      }
    }
  }

  return JPanel(GridLayout(1, 2)).also {
    it.add(initPanel("FlowLayout(LEFT)", panel1))
    it.add(initPanel("y=Math.pow(x/4.0,2.0)", panel2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initPanel(
  title: String,
  p: JComponent,
): Component {
  p.border = BorderFactory.createTitledBorder(title)
  p.add(JCheckBox("000000000000000000"))
  p.add(JCheckBox("11111111111"))
  p.add(JCheckBox("222222"))
  p.add(JCheckBox("3333333333"))
  p.add(JCheckBox("444444444444"))
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
