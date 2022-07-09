package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea()
  val key1 = "ColorChooser.swatchesRecentSwatchSize"
  log.append("$key1: ${UIManager.getDimension(key1)}\n")
  val key2 = "ColorChooser.swatchesSwatchSize"
  log.append("$key2: ${UIManager.getDimension(key2)}\n")

  UIManager.put(key1, Dimension(10, 8))
  UIManager.put(key2, Dimension(6, 10))

  val button1 = JButton("JColorChooser.showDialog(...)")
  button1.addActionListener {
    val color = JColorChooser.showDialog(null, "JColorChooser", null)
    log.append("color: $color\n")
  }

  val cc = JColorChooser()
  val dialog = JColorChooser.createDialog(
    null,
    "JST ColorChooserSwatchSize",
    true,
    cc,
    { log.append("ok\n") }
  ) { log.append("cancel\n") }
  val button2 = JButton("JColorChooser.createDialog(...).setVisible(true)")
  button2.addActionListener {
    dialog.isVisible = true
    log.append("color: ${cc.color}\n")
  }

  val p = JPanel()
  p.add(button1)
  p.add(button2)
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  p.preferredSize = Dimension(320, 240)
  return p
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
