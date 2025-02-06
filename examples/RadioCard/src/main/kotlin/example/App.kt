package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val title1 = "<html><h3>Button title 001</h3>"
  val help1 = "1234567890<br>123456789012345<br>1234567890"
  val b1 = JToggleButton(title1 + help1)
  val title2 = "<html><h3>Button title 002</h3>"
  val help2 = "123456789090"
  val b2 = JToggleButton(title2 + help2)
  val bg = ButtonGroup()
  bg.add(b1)
  bg.add(b2)
  val p = JPanel(GridLayout(1, 2, 5, 5))
  val help = "RadioCard with JToggleButton and JRadioButton"
  p.border = BorderFactory.createTitledBorder(help)
  p.add(makeRadioIconLayer(b1))
  p.add(makeRadioIconLayer(b2))
  return JPanel().also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRadioIconLayer(button: AbstractButton): Component {
  val radio = JRadioButton()
  radio.isOpaque = false
  val d = radio.preferredSize
  val i = button.insets
  button.margin = Insets(i.top, d.width, i.bottom, i.right)
  val dim = button.preferredSize
  button.preferredSize = dim // avoid button size shrinking
  button.verticalAlignment = SwingConstants.TOP
  val layer = object : LayerUI<AbstractButton>() {
    override fun paint(g: Graphics, c: JComponent) {
      super.paint(g, c)
      if (c is JLayer<*>) {
        val b = c.view
        if (b is AbstractButton) {
          val g2 = g.create() as? Graphics2D ?: return
          radio.isSelected = b.isSelected
          val x = i.left - d.width + 8
          val y = i.top + 8
          SwingUtilities.paintComponent(g2, radio, b, x, y, d.width, d.height)
          g2.dispose()
        }
      }
    }
  }
  return JLayer(button, layer)
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
