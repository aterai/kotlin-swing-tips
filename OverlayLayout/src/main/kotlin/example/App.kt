package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(JButton("JButton"))
  it.add(makeOverlayLayoutButton())
  it.preferredSize = Dimension(320, 240)
}

private fun makeOverlayLayoutButton(): Component {
  val b1 = JButton("OverlayLayoutButton")
  b1.layout = OverlayLayout(b1)
  b1.addActionListener { Toolkit.getDefaultToolkit().beep() }

  val i = b1.insets
  b1.border = BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, 4)
  val b2 = object : JButton("▼") {
    private val dim = Dimension(120, 24)
    override fun getPreferredSize() = dim

    override fun getMaximumSize() = preferredSize

    override fun getMinimumSize() = preferredSize
  }
  b2.addActionListener { JOptionPane.showMessageDialog(b2, "▼:sub") }
  b2.alignmentX = Component.RIGHT_ALIGNMENT
  b2.alignmentY = Component.BOTTOM_ALIGNMENT
  b1.add(b2)
  return b1
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
