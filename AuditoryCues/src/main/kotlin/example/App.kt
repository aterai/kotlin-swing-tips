package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

// Auditory Feedback for Swing Components - Swing Changes and New Features
// https://docs.oracle.com/javase/8/docs/technotes/guides/swing/1.4/SwingChanges.html#bug4290988
// Magic with Merlin: Swinging audio
// https://www.ibm.com/developerworks/java/library/j-mer0730/
class MainPanel : JPanel(GridLayout(2, 1, 5, 5)) {
  init {
    val button1 = JButton("showMessageDialog1")
    button1.addActionListener {
      UIManager.put("AuditoryCues.playList", AUDITORY_CUES)
      JOptionPane.showMessageDialog(this, "showMessageDialog1")
    }

    val button2 = JButton("showMessageDialog2")
    button2.addActionListener {
      UIManager.put("AuditoryCues.playList", UIManager.get("AuditoryCues.noAuditoryCues"))
      showMessageDialogAndPlayAudio(this, "showMessageDialog2", "notice2.wav")
    }

    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { getRootPane().setJMenuBar(mb) }

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(makeTitledPanel("Look&Feel Default", button1))
    add(makeTitledPanel("notice2.wav", button2))
    setPreferredSize(Dimension(320, 240))
  }

  private fun showMessageDialogAndPlayAudio(p: Component, msg: String, audioResource: String) {
    val url = MainPanel::class.java.getResource(audioResource)
    AudioSystem.getAudioInputStream(url).use { soundStream ->
      (AudioSystem.getLine(DataLine.Info(Clip::class.java, soundStream.getFormat())) as? Clip)?.use { clip ->
        val loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop()
        clip.addLineListener { e ->
          when (e.getType()) {
            LineEvent.Type.STOP, LineEvent.Type.CLOSE -> loop.exit()
          }
        }
        clip.open(soundStream)
        clip.start()
        JOptionPane.showMessageDialog(p, msg)
        loop.enter()
      }
    }
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(c)
  }

  companion object {
    val AUDITORY_CUES = arrayOf(
      "OptionPane.errorSound",
      "OptionPane.informationSound",
      "OptionPane.questionSound",
      "OptionPane.warningSound")
  }
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
internal object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafRadioGroup.getSelection()
      runCatching {
        setLookAndFeel(m.getActionCommand())
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
    }
    lafRadioGroup.add(lafItem)
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
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel)
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    // UIManager.put("AuditoryCues.playList", UIManager.get("AuditoryCues.allAuditoryCues"))
    // UIManager.put("AuditoryCues.playList", UIManager.get("AuditoryCues.defaultCueList"))
    // UIManager.put("AuditoryCues.playList", UIManager.get("AuditoryCues.noAuditoryCues"))
    UIManager.put("AuditoryCues.playList", MainPanel.AUDITORY_CUES)
    // UIManager.put("OptionPane.informationSound", "/example/notice2.wav")
    // UIManager.put("OptionPane.informationSound", "sounds/OptionPaneError.wav")
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
