package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalTabbedPaneUI

class MainPanel : JPanel(GridLayout(2, 1, 5, 5)) {
  init {
    UIManager.put("TabbedPane.tabRunOverlay", 0)
    UIManager.put("TabbedPane.selectedLabelShift", 0)
    UIManager.put("TabbedPane.labelShift", 0)
    UIManager.put("TabbedPane.selectedTabPadInsets", Insets(0, 0, 0, 0))

    val tabbedPane = object : JTabbedPane() {
      override fun updateUI() {
        super.updateUI()
        if (getUI() is WindowsTabbedPaneUI) {
          setUI(object : WindowsTabbedPaneUI() {
            override fun shouldRotateTabRuns(tabPlacement: Int) = false
          })
        } else {
          setUI(object : MetalTabbedPaneUI() {
            override fun shouldRotateTabRuns(tabPlacement: Int) = false

            override fun shouldRotateTabRuns(tabPlacement: Int, selectedRun: Int) = false
          })
        }
      }
    }

    add(makeTabbedPane(JTabbedPane()))
    add(makeTabbedPane(tabbedPane))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTabbedPane(tabbedPane: JTabbedPane) = tabbedPane.also {
    it.addTab("1111111111111111111111111111", ColorIcon(Color.RED), JLabel())
    it.addTab("2", ColorIcon(Color.GREEN), JLabel())
    it.addTab("333333333333333333333333333333333", ColorIcon(Color.BLUE), JLabel())
    it.addTab("444444444444", ColorIcon(Color.ORANGE), JLabel())
  }
}

class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(0, 0, getIconWidth(), getIconHeight())
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
