package example

// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@
import java.awt.*
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import javax.swing.*
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
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
        MainPanel::class.java.getResourceAsStream("button.xml").use({ input ->
          val synth = SynthLookAndFeel()
          synth.load(input, MainPanel::class.java)
          UIManager.setLookAndFeel(synth)
        })
//       try {
//         // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
//         MainPanel::class.java.getResourceAsStream("button.xml").use({ input ->
//           val synth = SynthLookAndFeel()
//           synth.load(input, MainPanel::class.java)
//           UIManager.setLookAndFeel(synth)
//         })
//       } catch (ex: ClassNotFoundException) {
//         ex.printStackTrace()
//       } catch (ex: InstantiationException) {
//         ex.printStackTrace()
//       } catch (ex: IllegalAccessException) {
//         ex.printStackTrace()
//       } catch (ex: UnsupportedLookAndFeelException) {
//         ex.printStackTrace()
//       }
      JFrame().apply {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        getContentPane().add(MainPanel())
        pack()
        setLocationRelativeTo(null)
        setVisible(true)
      }
    }
  })
}
