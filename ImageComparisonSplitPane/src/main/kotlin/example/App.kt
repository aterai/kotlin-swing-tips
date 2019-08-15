package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val split = JSplitPane()
  split.setContinuousLayout(true)
  split.setResizeWeight(.5)

  val check = JCheckBox("setXORMode(Color.BLUE)", true)
  check.addActionListener { split.repaint() }

  val cl = Thread.currentThread().getContextClassLoader()
  val icon = ImageIcon(cl.getResource("example/test.png"))

  val beforeCanvas = object : JComponent() {
    protected override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), this)
    }
  }
  split.setLeftComponent(beforeCanvas)

  val afterCanvas = object : JComponent() {
    protected override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as Graphics2D
      val iw = icon.getIconWidth()
      val ih = icon.getIconHeight()
      if (check.isSelected()) {
        g2.setColor(getBackground())
        g2.setXORMode(Color.BLUE)
      } else {
        g2.setPaintMode()
      }
      val pt = getLocation()
      g2.translate(-pt.x + split.getInsets().left, 0)
      g2.drawImage(icon.getImage(), 0, 0, iw, ih, this)
      g2.dispose()
    }
  }
  split.setRightComponent(afterCanvas)

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.add(check, BorderLayout.SOUTH)
    it.setOpaque(false)
    it.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
