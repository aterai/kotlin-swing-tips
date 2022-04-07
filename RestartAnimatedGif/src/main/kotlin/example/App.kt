package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val path = "example/9-0.gif"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val bi = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon9 = ImageIcon(bi)
  val animatedIcon = url?.let { ImageIcon(it) } ?: icon9
  val textArea = JTextArea()
  val button = object : JButton(icon9) {
    override fun fireStateChanged() {
      val m = getModel()
      if (isRolloverEnabled && m.isRollover) {
        textArea.append("JButton: Rollover, Image: flush\n")
        animatedIcon.image.flush()
      }
      super.fireStateChanged()
    }
  }
  button.rolloverIcon = animatedIcon
  button.pressedIcon = object : Icon {
    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color.BLACK
      g2.fillRect(x, y, iconWidth, iconHeight)
      g2.dispose()
    }

    override fun getIconWidth(): Int {
      return icon9.iconWidth
    }

    override fun getIconHeight(): Int {
      return icon9.iconHeight
    }
  }

  val label = JLabel(animatedIcon)
  label.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      textArea.append("JLabel: mousePressed, Image: flush\n")
      animatedIcon.image.flush()
      e.component.repaint()
    }
  })

  val p = JPanel(GridLayout(1, 2, 5, 5))
  p.add(makeTitledPanel("JButton#setRolloverIcon", button))
  p.add(makeTitledPanel("mousePressed: flush", label))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel().also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
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
