package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val palette = JInternalFrame("Palette", true, false, true, true)
  palette.setBounds(0, 0, 120, 120)
  palette.minimumSize = Dimension(50, 50)
  palette.putClientProperty("JInternalFrame.isPalette", true)
  palette.add(JScrollPane(JTree()))
  palette.isVisible = true

  val desktop = JDesktopPane()
  desktop.add(palette, JLayeredPane.PALETTE_LAYER)
  desktop.add(createFrame(0))
  desktop.add(createFrame(1))
  desktop.add(createFrame(2))

  val check = JCheckBox("Palette", true)
  check.addActionListener { e ->
    (e.source as? AbstractButton)?.also {
      palette.isVisible = it.isSelected
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(i: Int) = JInternalFrame(
  "title: $i",
  true,
  true,
  true,
  true,
).also {
  it.putClientProperty("JInternalFrame.isPalette", true)
  it.setSize(160, 120)
  it.setLocation(100 + 20 * i, 10 + 20 * i)
  EventQueue.invokeLater { it.isVisible = true }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
