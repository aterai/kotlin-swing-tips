package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI() = JPanel().also {
  it.add(JButton("JButton1"))
  it.add(JButton("JButton2"))
  it.add(MyButton("MyButton"))

  val button3 = JButton("JButton3")
  button3.name = "green:3"
  it.add(button3)

  val button4 = JButton("JButton4")
  button4.name = "green:4"
  it.add(button4)

  it.preferredSize = Dimension(320, 240)
}

private class MyButton(
  title: String,
) : JButton("$title: class")

fun main() {
  EventQueue.invokeLater {
    val cl = Thread.currentThread().contextClassLoader
    cl.getResourceAsStream("example/button.xml").use { input ->
      val synth = SynthLookAndFeel()
      synth.load(input, MyButton::class.java)
      UIManager.setLookAndFeel(synth)
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
