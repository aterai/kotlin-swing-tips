package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

const val KEY = "AuditoryCues.playList"
val AUDITORY_CUES = arrayOf(
  "OptionPane.errorSound",
  "OptionPane.informationSound",
  "OptionPane.questionSound",
  "OptionPane.warningSound"
)

fun makeUI(): Component {
  val b1 = JButton("showMessageDialog1")
  b1.addActionListener {
    UIManager.put(KEY, AUDITORY_CUES)
    JOptionPane.showMessageDialog(b1.rootPane, "showMessageDialog1")
  }

  val b2 = JButton("showMessageDialog2")
  b2.addActionListener {
    UIManager.put(KEY, UIManager.get("AuditoryCues.noAuditoryCues"))
    showMessageDialogAndPlayAudio(b2.rootPane, "showMessageDialog2", "example/notice2.wav")
  }

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }

    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("Look&Feel Default", b1))
    it.add(makeTitledPanel("notice2.wav", b2))
    it.preferredSize = Dimension(320, 240)
  }
}

fun showMessageDialogAndPlayAudio(p: Component, msg: String, audioResource: String) {
  val url = Thread.currentThread().contextClassLoader.getResource(audioResource) ?: return
  AudioSystem.getAudioInputStream(url).use { stream ->
    (AudioSystem.getLine(DataLine.Info(Clip::class.java, stream.format)) as? Clip)?.use {
      val loop = p.toolkit.systemEventQueue.createSecondaryLoop()
      it.addLineListener { e ->
        when (e.type) {
          LineEvent.Type.STOP, LineEvent.Type.CLOSE -> loop.exit()
        }
      }
      it.open(stream)
      it.start()
      JOptionPane.showMessageDialog(p, msg)
      loop.enter()
    }
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    UIManager.put(KEY, AUDITORY_CUES)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
