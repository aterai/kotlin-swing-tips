package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val combo0 = makeComboBox()
  val combo1 = makeComboBox()
  combo1.isLightWeightPopupEnabled = false

  val box = Box.createVerticalBox()
  box.add(combo0)
  box.add(combo1)
  box.add(Box.createVerticalGlue())

  val popup0 = makePopupMenu()
  val label0 = makeLabel("setLightWeightPopupEnabled: true", Color.ORANGE)
  label0.componentPopupMenu = popup0

  val popup1 = makePopupMenu()
  popup1.isLightWeightPopupEnabled = false
  val label1 = makeLabel("setLightWeightPopupEnabled: false", Color.PINK)
  label1.componentPopupMenu = popup1

  val glass = object : JPanel(GridLayout(4, 1, 5, 5)) {
    private val backgroundColor = Color(0x64_64_64_C8, true)

    override fun paintComponent(g: Graphics) {
      g.color = backgroundColor
      g.fillRect(0, 0, width, height)
      super.paintComponent(g)
    }
  }
  glass.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  glass.isOpaque = false
  glass.add(box)
  glass.add(label0)
  glass.add(label1)
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater {
      it.rootPane.glassPane = glass
      it.rootPane.glassPane.isVisible = true
    }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopupMenu() = JPopupMenu().also {
  it.add("JMenuItem")
  it.addSeparator()
  it.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  it.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  val menu = JMenu("JMenu")
  menu.add("JMenuItem1")
  menu.add("JMenuItem2")
  it.add(menu)
}

private fun makeComboBox() = JComboBox(arrayOf("Item1", "Item2", "Item3"))

private fun makeLabel(title: String, color: Color) = JLabel(title).also {
  it.isOpaque = true
  it.background = color
  it.toolTipText = "ToolTipText"
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    val frame = JFrame()
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.contentPane.add(makeUI())
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
}
