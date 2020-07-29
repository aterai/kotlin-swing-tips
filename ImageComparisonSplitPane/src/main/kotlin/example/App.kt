package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val split = JSplitPane()
  split.isContinuousLayout = true
  split.resizeWeight = .5

  val check = JCheckBox("setXORMode(Color.BLUE)", true)
  check.addActionListener { split.repaint() }

  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/test.png"))

  val beforeCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.drawImage(icon.image, 0, 0, icon.iconWidth, icon.iconHeight, this)
    }
  }
  split.leftComponent = beforeCanvas

  val afterCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      val iw = icon.iconWidth
      val ih = icon.iconHeight
      if (check.isSelected) {
        g2.color = background
        g2.setXORMode(Color.BLUE)
      } else {
        g2.setPaintMode()
      }
      val pt = location
      g2.translate(-pt.x + split.insets.left, 0)
      g2.drawImage(icon.image, 0, 0, iw, ih, this)
      g2.dispose()
    }
  }
  split.rightComponent = afterCanvas

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.add(check, BorderLayout.SOUTH)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
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
