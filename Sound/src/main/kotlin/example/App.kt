package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val b1 = JButton("play")
  b1.addActionListener { loadAndPlayAudio("example/notice1.wav") }

  val b2 = JButton("play")
  b2.addActionListener { loadAndPlayAudio("example/notice2.wav") }

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("notice1.wav", b1))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("notice2.wav", b2))
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun loadAndPlayAudio(path: String) {
  val url = Thread.currentThread().contextClassLoader.getResource(path) ?: return
  runCatching {
    AudioSystem.getAudioInputStream(url).use { sound ->
      val audio = AudioSystem.getLine(DataLine.Info(Clip::class.java, sound.format))
      (audio as? Clip)?.use { clip ->
        val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
        clip.addLineListener { e ->
          val t = e.type
          if (t == LineEvent.Type.STOP || t == LineEvent.Type.CLOSE) {
            loop.exit()
          }
        }
        clip.open(sound)
        clip.start()
        loop.enter()
      }
    }
  }.onFailure {
    Toolkit.getDefaultToolkit().beep()
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
