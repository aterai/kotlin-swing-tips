package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val desktop = JDesktopPane()

  val im = desktop.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  val modifiers = InputEvent.CTRL_DOWN_MASK
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, modifiers), "shrinkUp")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, modifiers), "shrinkDown")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, modifiers), "shrinkLeft")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, modifiers), "shrinkRight")

  addFrame(desktop, 0, true)
  addFrame(desktop, 1, false)
  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addFrame(desktop: JDesktopPane, idx: Int, resizable: Boolean) {
  val frame = JInternalFrame("resizable: $resizable", resizable, true, true, true)
  frame.add(makePanel())
  frame.setSize(240, 100)
  frame.setLocation(10 + 60 * idx, 10 + 120 * idx)
  desktop.add(frame)
  EventQueue.invokeLater { frame.isVisible = true }
}

private fun makePanel() = JPanel().also {
  it.add(JLabel("label"))
  it.add(JButton("button"))
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
