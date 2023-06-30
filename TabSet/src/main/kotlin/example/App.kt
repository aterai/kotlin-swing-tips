package example

import java.awt.*
import javax.swing.*
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.TabSet
import javax.swing.text.TabStop

fun makeUI(): Component {
  val check = JCheckBox("vertical grid lines", true)
  check.addActionListener { e -> (e.source as? JComponent)?.rootPane?.repaint() }

  val textPane = object : JTextPane() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (check.isSelected) {
        val ox = insets.left
        val h = height
        g.color = Color.RED
        g.drawLine(ox, 0, ox, h)
        g.drawLine(ox + 100, 0, ox + 100, h)
        g.drawLine(ox + 200, 0, ox + 200, h)
        g.drawLine(ox + 300, 0, ox + 300, h)
        g.color = Color.ORANGE
        g.drawLine(ox + 50, 0, ox + 50, h)
        g.drawLine(ox + 150, 0, ox + 150, h)
        g.drawLine(ox + 250, 0, ox + 250, h)
      }
    }
  }
  textPane.text = """
    LEFT1	CENTER1	RIGHT1	3.14
    LEFT22	CENTER22	RIGHT22	12.3
    LEFT333	CENTER333	RIGHT333	123.45
    LEFT4444	CENTER4444	RIGHT4444	0.9876
  """.trimIndent()

  val array = arrayOf(
    TabStop(0f, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
    TabStop(100f, TabStop.ALIGN_CENTER, TabStop.LEAD_NONE),
    TabStop(200f, TabStop.ALIGN_RIGHT, TabStop.LEAD_NONE),
    TabStop(250f, TabStop.ALIGN_DECIMAL, TabStop.LEAD_NONE)
  )
  val attr = textPane.getStyle(StyleContext.DEFAULT_STYLE)
  StyleConstants.setTabSet(attr, TabSet(array))
  textPane.setParagraphAttributes(attr, false)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.add(check, BorderLayout.SOUTH)
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
