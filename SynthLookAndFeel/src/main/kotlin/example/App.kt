package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.synth.SynthLookAndFeel

class MainPanel : JPanel() {
  init {
    add(JButton("JButton1"))
    add(JButton("JButton2"))
    add(MyButton("MyButton"))

    val button3 = JButton("JButton3")
    button3.setName("green:3")
    add(button3)

    val button4 = JButton("JButton4")
    button4.setName("green:4")
    add(button4)

    setPreferredSize(Dimension(320, 240))
  }
}

class MyButton(title: String) : JButton("$title: class")

fun main() {
  EventQueue.invokeLater {
    MainPanel::class.java.getResourceAsStream("button.xml").use { input ->
      val synth = SynthLookAndFeel()
      synth.load(input, MainPanel::class.java)
      UIManager.setLookAndFeel(synth)
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
