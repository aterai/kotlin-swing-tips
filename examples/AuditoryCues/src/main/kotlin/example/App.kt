package example

import java.awt.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.swing.*

const val KEY = "AuditoryCues.playList"
val AUDITORY_CUES = arrayOf(
  "OptionPane.errorSound",
  "OptionPane.informationSound",
  "OptionPane.questionSound",
  "OptionPane.warningSound",
)

fun makeUI(): Component {
  val b1 = JButton("showMessageDialog1")
  b1.addActionListener {
    UIManager.put(KEY, AUDITORY_CUES)
    val msg = "showMessageDialog1"
    JOptionPane.showMessageDialog(b1.rootPane, msg)
  }

  val b2 = JButton("showMessageDialog2")
  b2.addActionListener {
    UIManager.put(KEY, UIManager.get("AuditoryCues.noAuditoryCues"))
    val msg = "showMessageDialog2"
    showMessageDialogAndPlayAudio(b2.rootPane, msg, "example/notice2.wav")
  }

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }

    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("Look&Feel Default", b1))
    it.add(makeTitledPanel("notice2.wav", b2))
    it.preferredSize = Dimension(320, 240)
  }
}

fun showMessageDialogAndPlayAudio(
  p: Component,
  msg: String,
  audioPath: String,
) {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource(audioPath) ?: return
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

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
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
