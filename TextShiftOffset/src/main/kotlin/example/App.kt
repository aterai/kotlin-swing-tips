package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("Button.textShiftOffset", 0)
  val rl = listOf(
    JRadioButton(TextShiftOffsetAction(0)),
    JRadioButton(TextShiftOffsetAction(1)),
    JRadioButton(TextShiftOffsetAction(-1)),
  )
  val bg = ButtonGroup()
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createTitledBorder("UIManager.put(\"Button.textShiftOffset\", offset)")
  box.add(JLabel("offset = "))

  var isFirst = true
  for (rb in rl) {
    if (isFirst) {
      rb.isSelected = true
      isFirst = false
    }
    bg.add(rb)
    box.add(rb)
    box.add(Box.createHorizontalStrut(5))
  }
  box.add(Box.createHorizontalGlue())

  val p = JPanel()
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(JButton("JButton"))
  p.add(JButton(CloseIcon()))
  p.add(JButton("<html>JButton<br>html<br>tag<br>test"))
  p.add(JToggleButton("JToggleButton"))

  return JPanel(BorderLayout(5, 5)).also {
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextShiftOffsetAction(private val offset: Int) : AbstractAction(" $offset ") {
  override fun actionPerformed(e: ActionEvent) {
    UIManager.put("Button.textShiftOffset", offset)
    (e.source as? JComponent)?.also {
      SwingUtilities.updateComponentTreeUI(it.topLevelAncestor)
    }
  }
}

private class CloseIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.BLACK
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
