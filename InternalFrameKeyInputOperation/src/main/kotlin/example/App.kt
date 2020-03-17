package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val desktop = JDesktopPane()

    val im = desktop.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    val modifiers = InputEvent.CTRL_DOWN_MASK
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, modifiers), "shrinkUp")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, modifiers), "shrinkDown")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, modifiers), "shrinkLeft")
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, modifiers), "shrinkRight")

    addFrame(desktop, 0, true)
    addFrame(desktop, 1, false)
    add(desktop)
    setPreferredSize(Dimension(320, 240))
  }

  private fun addFrame(desktop: JDesktopPane, idx: Int, resizable: Boolean) {
    val frame = JInternalFrame("resizable: $resizable", resizable, true, true, true)
    frame.add(makePanel())
    frame.setSize(240, 100)
    frame.setVisible(true)
    frame.setLocation(10 + 60 * idx, 10 + 120 * idx)
    desktop.add(frame)
  }

  private fun makePanel() = JPanel().also {
    it.add(JLabel("label"))
    it.add(JButton("button"))
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      val mb = JMenuBar()
      mb.add(LookAndFeelUtil.createLookAndFeelMenu())
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(MainPanel())
      setJMenuBar(mb)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
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
