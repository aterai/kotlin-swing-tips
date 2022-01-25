package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val END_OF_TRACK: Byte = 0x2F
private val start = makeButton("start")
private val pause = makeButton("pause")
private val reset = makeButton("reset")
private var tickPos = 0L

fun makeUI(): Component {
  pause.isEnabled = false

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/Mozart_toruko_k.mid")
  val worker = object : SwingWorker<Void, Long>() {
    @Throws(InterruptedException::class)
    override fun doInBackground(): Void? {
      runCatching {
        MidiSystem.getSequencer().use { sequencer ->
          sequencer.open()
          sequencer.sequence = MidiSystem.getSequence(url)
          sequencer.addMetaEventListener { e ->
            if (e.type == END_OF_TRACK.toInt()) {
              publish(0L)
            }
          }
          addListener(sequencer)
          while (sequencer.isOpen) {
            if (sequencer.isRunning) {
              publish(sequencer.tickPosition)
            }
            Thread.sleep(1000)
          }
        }
      }.onFailure {
        publish(0L)
      }
      return null
    }

    private fun addListener(sequencer: Sequencer) {
      EventQueue.invokeLater {
        start.addActionListener {
          sequencer.tickPosition = tickPos
          sequencer.start()
          initButtons(false)
        }
        pause.addActionListener {
          publish(sequencer.tickPosition)
          sequencer.stop()
          initButtons(true)
        }
        reset.addActionListener {
          sequencer.stop()
          tickPos = 0
          initButtons(true)
        }
      }
    }

    override fun process(chunks: List<Long>) {
      updateButtons(chunks)
    }
  }
  worker.execute()

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 0, 0)
    it.add(Box.createHorizontalGlue())
    it.add(start)
    it.add(pause)
    it.add(reset)
  }

  return JPanel(BorderLayout(5, 5)).also {
    addHierarchyListener(it, worker)
    it.add(makeTitle(it.background))
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addHierarchyListener(it: JPanel, worker: SwingWorker<Void, Long>) {
  it.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      worker.cancel(true)
    }
  }
}

private fun updateButtons(chunks: List<Long>) {
  chunks.forEach {
    tickPos = it
    if (it == 0L) {
      initButtons(true)
    }
  }
}

private fun initButtons(flg: Boolean) {
  start.isEnabled = flg
  pause.isEnabled = !flg
}

private fun makeTitle(bgc: Color): Component {
  val txt = """Wolfgang Amadeus Mozart
    Piano Sonata No. 11 in A major, K 331
    (Turkish Rondo)
    """.trimMargin()
  val label = JTextArea(txt)
  label.border = BorderFactory.createTitledBorder("MIDI")
  label.isEditable = false
  label.background = bgc
  return label
}

private fun makeButton(title: String) = JButton(title).also {
  it.isFocusable = false
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
