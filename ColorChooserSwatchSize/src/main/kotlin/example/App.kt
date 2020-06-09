package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  println(UIManager.getDimension("ColorChooser.swatchesRecentSwatchSize"))
  println(UIManager.getDimension("ColorChooser.swatchesSwatchSize"))
  UIManager.put("ColorChooser.swatchesRecentSwatchSize", Dimension(10, 8))
  UIManager.put("ColorChooser.swatchesSwatchSize", Dimension(6, 10))

  val button1 = JButton("JColorChooser.showDialog(...)")
  button1.addActionListener {
    val color = JColorChooser.showDialog(null, "JColorChooser", null)
    println(color)
  }

  val cc = JColorChooser()
  val dialog = JColorChooser.createDialog(
    null, "JST ColorChooserSwatchSize", true, cc,
    { println("ok") }
  ) { println("cancel") }
  val button2 = JButton("JColorChooser.createDialog(...).setVisible(true)")
  button2.addActionListener {
    dialog.isVisible = true
    val color = cc.color
    println(color)
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
