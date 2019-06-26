package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val panel1 = JPanel(FlowLayout(FlowLayout.LEFT))
  val panel2 = object : JPanel() {
    protected val A2 = 4.0
    protected override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val i = getInsets()
      val g2 = g.create() as Graphics2D
      g2.translate(i.left, i.top)
      g2.setPaint(Color.RED)
      val w = getWidth() - i.left - i.right
      var px = 0
      var py = 0
      for (x in 0 until w) {
        val y = Math.pow(x / A2, 2.0).toInt()
        g2.drawLine(px, py, x, y)
        px = x
        py = y
      }
      g2.dispose()
    }

    override fun updateUI() {
      super.updateUI()
      setLayout(object : FlowLayout() {
        override fun layoutContainer(target: Container) {
          synchronized(target.getTreeLock()) {
            val nmembers = target.getComponentCount()
            if (nmembers <= 0) {
              return
            }
            val insets = target.getInsets()
            val vgap = getVgap()
            val hgap = getHgap()
            val rowh = (target.getHeight() - insets.top - insets.bottom - vgap * 2) / nmembers
            var x = insets.left + hgap
            var y = insets.top + vgap
            for (i in 0 until nmembers) {
              val m = target.getComponent(i)
              if (m.isVisible()) {
                val d = m.getPreferredSize()
                m.setSize(d.width, d.height)
                m.setLocation(x, y)
                y += vgap + minOf(rowh, d.height)
                x = (A2 * Math.sqrt(y.toDouble())).toInt()
              }
            }
          }
        }
      })
    }
  }

  return JPanel(GridLayout(1, 2)).also {
    it.add(initPanel("FlowLayout(LEFT)", panel1))
    it.add(initPanel("y=Math.pow(x/4.0,2.0)", panel2))
    it.setPreferredSize(Dimension(320, 240))
  }
}

private fun initPanel(title: String, p: JComponent): Component {
  p.setBorder(BorderFactory.createTitledBorder(title))
  p.add(JCheckBox("aaaaaaaaaaaaaaa"))
  p.add(JCheckBox("bbbbbbbb"))
  p.add(JCheckBox("ccccccccccc"))
  p.add(JCheckBox("ddddddd"))
  p.add(JCheckBox("eeeeeeeeeee"))
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
