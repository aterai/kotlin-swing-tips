package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.add(makeLabel("Color(0xFF0000)", Color(0xFF0000)))
  box.add(makeLabel("Color(0x88_88_88)", Color(0x88_88_88)))
  box.add(makeLabel("Color(\"00FF00\".toInt(16))", Color("00FF00".toInt(16))))
  box.add(makeLabel("Color(Integer.decode(\"#0000FF\"))", Color(Integer.decode("#0000FF"))))
  box.add(makeLabel("Color.decode(\"#00FFFF\")", Color.decode("#00FFFF")))

  val label = JLabel("<html><span style='color: #FF00FF'>#FF00FF")
  label.border = BorderFactory.createTitledBorder("new JLabel(\"<html><span style='color: #FF00FF'>#FF00FF\")")
  box.add(label)
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(box))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabel(title: String, c: Color): JLabel {
  val label = object : JLabel("#%06x".format(c.rgb and 0xFFFFFF)) {
    override fun getMaximumSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = Short.MAX_VALUE.toInt()
      return d
    }
  }
  label.border = BorderFactory.createTitledBorder(title)
  label.foreground = c
  return label
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
