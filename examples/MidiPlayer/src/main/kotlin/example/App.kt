package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.net.URL
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.swing.*

private val slider = JSlider(0, 100, 0)
private var sequencer: Sequencer? = null
private var isMovingSlider = false

fun createUI(): Component {
  val cl = Thread.currentThread().getContextClassLoader()
  val url = cl.getResource("example/Mozart_toruko_k.mid")
  initSequencer(url)

  val timer = Timer(50) {
    if (!isMovingSlider && sequencer?.isRunning == true) {
      slider.setValue(sequencer?.tickPosition?.toInt() ?: 0)
    }
  }
  timer.start()

  slider.addHierarchyListener { e ->
    val flg = e.getChangeFlags() and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()
    if (flg != 0L && !e.component.isDisplayable) {
      sequencer?.close()
      timer.stop()
    }
  }

  slider.addChangeListener {
    if (slider.valueIsAdjusting) {
      isMovingSlider = true
    } else if (isMovingSlider) {
      sequencer?.tickPosition = slider.value.toLong()
      isMovingSlider = false
    }
  }

  val playButton = JButton("Play")
  playButton.setEnabled(url != null)
  playButton.addActionListener {
    if (sequencer?.isRunning == false) {
      sequencer?.start()
    }
  }

  val box = Box.createHorizontalBox()
  box.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0))
  box.add(slider)
  box.add(Box.createHorizontalStrut(2))
  box.add(playButton)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(createTitleBox(it.getBackground()))
    it.add(box, BorderLayout.SOUTH)
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initSequencer(url: URL?) {
  runCatching {
    sequencer = MidiSystem.getSequencer().also {
      if (url != null) {
        it.open()
        it.sequence = MidiSystem.getSequence(url)
        slider.setMaximum(it.tickLength.toInt())
      }
    }
  }
}

private fun createTitleBox(bgc: Color): Component {
  val txt = """
    Wolfgang Amadeus Mozart
    Piano Sonata No. 11 in A major, K 331
    (Turkish Rondo)
  """.trimIndent()
  val label = JTextArea(txt)
  label.setBorder(BorderFactory.createTitledBorder("MIDI"))
  label.isEditable = false
  label.setBackground(bgc)
  return label
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
