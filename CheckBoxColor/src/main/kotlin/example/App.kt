package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.metal.MetalCheckBoxIcon

fun makeUI(): Component {
  val cb1 = JCheckBox("111111")
  cb1.icon = CheckBoxIcon()
  val cb2 = JCheckBox("222222222222")
  cb2.icon = CheckBoxIcon2()
  val cb3 = JCheckBox("333333333")
  cb3.icon = CheckBoxIcon3()
  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default", JCheckBox("000000000")))
  box.add(makeTitledPanel("WindowsIconFactory", cb1))
  box.add(makeTitledPanel("CheckBox.icon+RED", cb2))
  box.add(makeTitledPanel("MetalCheckBoxIcon+GRAY", cb3))
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class CheckBoxIcon3 : Icon {
  private val orgIcon = MetalCheckBoxIcon()

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    orgIcon.paintIcon(c, g2, 0, 0)
    g2.color = Color(255, 155, 155, 100)
    g2.fillRect(2, 2, iconWidth - 4, iconHeight - 4)
    g2.dispose()
  }

  override fun getIconWidth() = orgIcon.iconWidth

  override fun getIconHeight() = orgIcon.iconHeight
}

private class CheckBoxIcon2 : Icon {
  private val orgIcon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    orgIcon.paintIcon(c, g2, 0, 0)
    if (c is AbstractButton) {
      val model = c.model
      g2.color = Color(255, 155, 155, 100)
      g2.fillRect(2, 2, iconWidth - 4, iconHeight - 4)
      if (model.isSelected) {
        g2.color = Color.RED
        g2.drawLine(9, 3, 9, 3)
        g2.drawLine(8, 4, 9, 4)
        g2.drawLine(7, 5, 9, 5)
        g2.drawLine(6, 6, 8, 6)
        g2.drawLine(3, 7, 7, 7)
        g2.drawLine(4, 8, 6, 8)
        g2.drawLine(5, 9, 5, 9)
        g2.drawLine(3, 5, 3, 5)
        g2.drawLine(3, 6, 4, 6)
      }
    }
    g2.dispose()
  }

  override fun getIconWidth() = orgIcon.iconWidth

  override fun getIconHeight() = orgIcon.iconHeight
}

private class CheckBoxIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val cb = c as? JCheckBox ?: return
    val model = cb.model
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)

    // outer bevel
    if (cb.isBorderPaintedFlat) {
      g2.color = UIManager.getColor("CheckBox.shadow")
      g2.drawRect(1, 1, SIZE - 3, SIZE - 3)
      if (model.isPressed && model.isArmed) {
        g2.color = UIManager.getColor("CheckBox.background")
      } else {
        g2.color = UIManager.getColor("CheckBox.interiorBackground")
      }
      g2.fillRect(2, 2, SIZE - 4, SIZE - 4)
    } else {
      // Outer top/left
      g2.color = UIManager.getColor("CheckBox.shadow")
      g2.drawLine(0, 0, 11, 0)
      g2.drawLine(0, 1, 0, 11)

      // Outer bottom/right
      g2.color = UIManager.getColor("CheckBox.highlight")
      g2.drawLine(12, 0, 12, 12)
      g2.drawLine(0, 12, 11, 12)

      // Inner top.left
      g2.color = UIManager.getColor("CheckBox.darkShadow")
      g2.drawLine(1, 1, 10, 1)
      g2.drawLine(1, 2, 1, 10)

      // Inner bottom/right
      g2.color = UIManager.getColor("CheckBox.light")
      g2.drawLine(1, 11, 11, 11)
      g2.drawLine(11, 1, 11, 10)

      // inside box
      val color = Color(255, 155, 155)
      if (model.isPressed && model.isArmed) {
        g2.color = color.brighter()
      } else {
        g2.color = color
      }
      g2.fillRect(2, 2, SIZE - 4, SIZE - 4)
    }

    // paint check
    if (model.isSelected) {
      g2.color = Color.BLUE
      g2.drawLine(9, 3, 9, 3)
      g2.drawLine(8, 4, 9, 4)
      g2.drawLine(7, 5, 9, 5)
      g2.drawLine(6, 6, 8, 6)
      g2.drawLine(3, 7, 7, 7)
      g2.drawLine(4, 8, 6, 8)
      g2.drawLine(5, 9, 5, 9)
      g2.drawLine(3, 5, 3, 5)
      g2.drawLine(3, 6, 4, 6)
    }
    if (model.isRollover) {
      g2.color = Color.ORANGE
      g2.drawLine(1, 1, 1 + SIZE - 3, 1)
      g2.drawLine(1, 1, 1, 1 + SIZE - 3)
    }
    g2.dispose()
  }

  override fun getIconWidth() = SIZE

  override fun getIconHeight() = SIZE

  companion object {
    private const val SIZE = 13
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
