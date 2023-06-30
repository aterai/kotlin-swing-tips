package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/wi0124-48.png")
  val icon = ImageIcon(url)
  val l1 = object : JLabel("ToolTip icon using JLabel") {
    override fun createToolTip(): JToolTip {
      val iconLabel = JLabel(icon)
      iconLabel.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
      val tip = object : JToolTip() {
        override fun getPreferredSize() = layout.preferredLayoutSize(this)

        override fun setTipText(tipText: String) {
          val oldValue = iconLabel.text
          iconLabel.text = tipText
          firePropertyChange("tip text", oldValue, tipText)
        }
      }
      tip.component = this
      tip.layout = BorderLayout()
      tip.add(iconLabel)
      return tip
    }
  }
  l1.toolTipText = "Test1"

  val l2 = object : JLabel("ToolTip icon using MatteBorder") {
    override fun createToolTip(): JToolTip {
      val tip = object : JToolTip() {
        override fun getPreferredSize() = super.getPreferredSize().also {
          val i = insets
          it.height = it.height.coerceAtLeast(icon.iconHeight + i.top + i.bottom)
        }
      }
      tip.component = this
      val b1 = tip.border
      val b2 = BorderFactory.createMatteBorder(0, icon.iconWidth, 0, 0, icon)
      val b3 = BorderFactory.createEmptyBorder(1, 1, 1, 1)
      val b4 = BorderFactory.createCompoundBorder(b3, b2)
      tip.border = BorderFactory.createCompoundBorder(b1, b4)
      return tip
    }
  }
  l2.toolTipText = "Test2"

  val l3 = JLabel("ToolTip icon using HTML tags")
  l3.toolTipText = "<html><img src='$url'>Test3</img></html>"

  val box = Box.createVerticalBox()
  box.add(l1)
  box.add(Box.createVerticalStrut(20))
  box.add(l2)
  box.add(Box.createVerticalStrut(20))
  box.add(l3)
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(20, 40, 20, 40)
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
