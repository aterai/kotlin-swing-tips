package example

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val SHOW_MNEMONICS = "Button.showMnemonics"

fun makeUI(): Component {
  val showMnemonicsCheck = JCheckBox(SHOW_MNEMONICS)
  showMnemonicsCheck.isSelected = UIManager.getBoolean(SHOW_MNEMONICS)
  showMnemonicsCheck.mnemonic = KeyEvent.VK_B
  showMnemonicsCheck.addActionListener { e ->
    UIManager.put(SHOW_MNEMONICS, (e.source as? JCheckBox)?.isSelected == true)
    if (UIManager.getLookAndFeel() is WindowsLookAndFeel) {
      WindowsLookAndFeel.setMnemonicHidden(true)
      showMnemonicsCheck.topLevelAncestor?.repaint()
    }
  }

  val button = JButton("JButton")
  button.mnemonic = KeyEvent.VK_D

  return JPanel().also {
    it.add(showMnemonicsCheck)
    it.add(button)
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar() = JMenuBar().also {
  it.add(createMenu("File", listOf("Open", "Save", "Exit")))
  it.add(createMenu("Edit", listOf("Cut", "Copy", "Paste", "Delete")))
  it.add(LookAndFeelUtil.createLookAndFeelMenu())
  it.add(Box.createGlue())
  it.add(createMenu("Help", listOf("Version", "About")))
}

private fun createMenu(title: String, list: List<String>) = JMenu(title).also {
  it.mnemonic = title.codePointAt(0)
  for (s in list) {
    it.add(s).mnemonic = s.codePointAt(0)
  }
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
