package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI

fun makeUI(): Component {
  UIManager.put("ComboBox.foreground", Color.WHITE)
  UIManager.put("ComboBox.background", Color.BLACK)
  UIManager.put("ComboBox.selectionForeground", Color.CYAN)
  UIManager.put("ComboBox.selectionBackground", Color.BLACK)
  UIManager.put("ComboBox.buttonDarkShadow", Color.BLACK)
  UIManager.put("ComboBox.buttonBackground", Color.WHITE)
  UIManager.put("ComboBox.buttonHighlight", Color.WHITE)
  UIManager.put("ComboBox.buttonShadow", Color.WHITE)
  UIManager.put("ComboBox.border", BorderFactory.createLineBorder(Color.WHITE))
  UIManager.put("ComboBox.editorBorder", BorderFactory.createLineBorder(Color.GREEN))
  UIManager.put("TitledBorder.titleColor", Color.WHITE)
  UIManager.put("TitledBorder.border", BorderFactory.createEmptyBorder())
  val combo00 = makeComboBox()
  val o00 = combo00.accessibleContext.getAccessibleChild(0)
  (o00 as? JComponent)?.border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.WHITE)
  val combo01 = makeComboBox()
  combo01.setUI(BasicComboBoxUI())
  val o01 = combo01.accessibleContext.getAccessibleChild(0)
  (o01 as? JComponent)?.border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.WHITE)
  val combo02 = makeComboBox()
  val ui2 = object : BasicComboBoxUI() {
    override fun createArrowButton(): JButton {
      val b = JButton(ArrowIcon()) // createArrowButton()
      b.background = Color.BLACK
      b.isContentAreaFilled = false
      b.isFocusPainted = false
      b.border = BorderFactory.createEmptyBorder()
      return b
    }
  }
  combo02.setUI(ui2)
  val ml2 = object : MouseAdapter() {
    private fun getButtonModel(e: MouseEvent): ButtonModel? {
      val b = (e.component as? JComboBox<*>)?.getComponent(0)
      return (b as? JButton)?.model
    }

    override fun mouseEntered(e: MouseEvent) {
      getButtonModel(e)?.isRollover = true
    }

    override fun mouseExited(e: MouseEvent) {
      getButtonModel(e)?.isRollover = false
    }

    override fun mousePressed(e: MouseEvent) {
      getButtonModel(e)?.isPressed = true
    }

    override fun mouseReleased(e: MouseEvent) {
      getButtonModel(e)?.isPressed = false
    }
  }
  combo02.addMouseListener(ml2)
  val o02 = combo02.accessibleContext.getAccessibleChild(0)
  (o02 as? JComponent)?.border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.WHITE)
  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("MetalComboBoxUI:", combo00))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("BasicComboBoxUI:", combo01))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("BasicComboBoxUI#createArrowButton():", combo02))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.isOpaque = true
    it.background = Color.BLACK
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  p.isOpaque = true
  p.background = Color.BLACK
  return p
}

private fun makeComboBox(): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("1234")
  model.addElement("5555555555555555555555")
  model.addElement("6789000000000")
  return JComboBox(model)
}

private class ArrowIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color.WHITE
    var shift = 0
    if (c is AbstractButton) {
      val m = c.model
      if (m.isPressed) {
        shift = 1
      } else {
        if (m.isRollover) {
          g2.paint = Color.WHITE
        } else {
          g2.paint = Color.BLACK
        }
      }
    }
    g2.translate(x, y + shift)
    g2.drawLine(2, 3, 6, 3)
    g2.drawLine(3, 4, 5, 4)
    g2.drawLine(4, 5, 4, 5)
    g2.dispose()
  }

  override fun getIconWidth() = 9

  override fun getIconHeight() = 9
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
