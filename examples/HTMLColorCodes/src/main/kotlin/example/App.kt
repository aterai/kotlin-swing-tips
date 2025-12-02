package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val l1 = mkLbl("Color(0xFF0000)", Color(0xFF0000))
  val l2 = mkLbl("Color(0x88_88_88)", Color(0x88_88_88))
  val l3 = mkLbl("Color('00FF00'.toInt(16))", Color("00FF00".toInt(16)))
  val l4 = mkLbl("Color(Integer.decode('#0000FF'))", Color(Integer.decode("#0000FF")))
  val l5 = mkLbl("Color.decode('#00FFFF')", Color.decode("#00FFFF"))
  val title = "<html><span style='color: #FF00FF'>#FF00FF"
  val l6 = JLabel(title)
  l6.border = BorderFactory.createTitledBorder("JLabel($title)")
  val box = Box.createVerticalBox()
  listOf(l1, l2, l3, l4, l5, l6).forEach { box.add(it) }
  box.add(Box.createVerticalGlue())
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(box))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun mkLbl(
  title: String,
  c: Color,
): JLabel {
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
