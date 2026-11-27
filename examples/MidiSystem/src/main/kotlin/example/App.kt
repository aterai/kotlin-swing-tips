package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.util.logging.Logger
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.swing.*

private const val END_OF_TRACK = 0x2F
private val start = createButton("start")
private val pause = createButton("pause")
private val reset = createButton("reset")
private var tickPos = 0L

fun createUI(): Component {
  pause.isEnabled = false

  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/Mozart_toruko_k.mid")
  val worker = object : SwingWorker<Unit?, Long>() {
    @Throws(InterruptedException::class)
    override fun doInBackground(): Unit? {
      runCatching {
        MidiSystem.getSequencer().use { sequencer ->
          sequencer.open()
          sequencer.sequence = MidiSystem.getSequence(url)
          sequencer.addMetaEventListener { e ->
            if (e.type == END_OF_TRACK) {
              publish(0L)
            }
          }
          addListener(sequencer)
          // Keep the sequencer open until this worker is cancelled
          // (Thread#sleep throws InterruptedException on cancel(true)).
          while (sequencer.isOpen) {
            Thread.sleep(1000)
          }
        }
      }.onFailure {
        Logger.getGlobal().severe { it.message }
        publish(0L)
      }
      return null
    }

    private fun addListener(sequencer: Sequencer) {
      EventQueue.invokeLater {
        start.addActionListener {
          sequencer.tickPosition = tickPos
          sequencer.start()
          updateButtons(false)
        }
        pause.addActionListener {
          sequencer.stop()
          // Save the position directly on the EDT instead of via publish(...),
          // so that a later start always resumes from the paused position.
          tickPos = sequencer.tickPosition
          updateButtons(true)
        }
        reset.addActionListener {
          sequencer.stop()
          tickPos = 0
          updateButtons(true)
        }
      }
    }

    // Only called when the end of the track is reached (or loading failed)
    override fun process(chunks: List<Long>) {
      tickPos = 0L
      updateButtons(true)
    }

    override fun done() {
      tickPos = 0L
      updateButtons(true)
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
    it.add(createTitle(it.background))
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addHierarchyListener(
  panel: JPanel,
  worker: SwingWorker<*, *>,
) {
  panel.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      worker.cancel(true)
    }
  }
}

private fun updateButtons(stopped: Boolean) {
  start.isEnabled = stopped
  pause.isEnabled = !stopped
}

private fun createTitle(bgc: Color): Component {
  val txt = """
    Wolfgang Amadeus Mozart
    Piano Sonata No. 11 in A major, K 331
    (Turkish Rondo)
  """.trimMargin()
  val label = JTextArea(txt)
  label.border = BorderFactory.createTitledBorder("MIDI")
  label.isEditable = false
  label.background = bgc
  return label
}

private fun createButton(title: String) = JButton(title).also {
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
