package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val outside = BorderFactory.createMatteBorder(0, 10, 1, 0, Color(0x32_C8_32))
  val inside = BorderFactory.createEmptyBorder(0, 5, 0, 0)
  val label = JLabel("MANIFEST.MF")
  label.border = BorderFactory.createCompoundBorder(outside, inside)
  val font = label.font
  label.font = Font(font.fontName, font.style, font.size * 2)
  val p = JPanel(BorderLayout(2, 2))
  p.add(label, BorderLayout.NORTH)
  p.add(makeInfoBox(), BorderLayout.SOUTH)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeInfoBox(): Box {
  val box = Box.createVerticalBox()
  box.add(JLabel("Manifest-Version: 1.0"))
  box.add(JLabel("Ant-Version: Apache Ant 1.6.2"))
  box.add(JLabel("Created-By: 1.4.2_06-b03 (Sun Microsystems Inc.)"))
  box.add(JLabel("Main-Class: example.MainPanel"))
  box.add(JLabel("Implementation-Title: Example"))
  box.add(JLabel("Implementation-Version: 1.0.32"))
  box.add(JLabel("Class-Path: ."))
  val box2 = Box.createHorizontalBox()
  box2.add(Box.createHorizontalStrut(10))
  box2.add(box)
  return box2
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
