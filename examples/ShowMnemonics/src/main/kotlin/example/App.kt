package example

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*

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
  it.add(LookAndFeelUtils.createLookAndFeelMenu())
  it.add(Box.createGlue())
  it.add(createMenu("Help", listOf("Version", "About")))
}

private fun createMenu(title: String, list: List<String>) = JMenu(title).also {
  it.mnemonic = title.codePointAt(0)
  for (s in list) {
    it.add(s).mnemonic = s.codePointAt(0)
  }
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
