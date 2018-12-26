package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*

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

  private fun makePanel(): Component {
    val p = JPanel()
    p.add(JLabel("label"))
    p.add(JButton("button"))
    return p
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
      JFrame().apply {
        val mb = JMenuBar()
        mb.add(LookAndFeelUtil.createLookAndFeelMenu())
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        getContentPane().add(MainPanel())
        setJMenuBar(mb)
        pack()
        setLocationRelativeTo(null)
        setVisible(true)
      }
    }
  })
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
internal object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()
  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      menu.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
    return menu
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JRadioButtonMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener({
      val m = lafRadioGroup.getSelection()
      try {
        setLookAndFeel(m.getActionCommand())
      } catch (ex: ClassNotFoundException) {
        ex.printStackTrace()
      } catch (ex: InstantiationException) {
        ex.printStackTrace()
      } catch (ex: IllegalAccessException) {
        ex.printStackTrace()
      } catch (ex: UnsupportedLookAndFeelException) {
        ex.printStackTrace()
      }
    })
    lafRadioGroup.add(lafItem)
    return lafItem
  }

  @Throws(ClassNotFoundException::class, InstantiationException::class, IllegalAccessException::class, UnsupportedLookAndFeelException::class)
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }
  }

  private fun updateLookAndFeel() {
    for (window in Frame.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */
