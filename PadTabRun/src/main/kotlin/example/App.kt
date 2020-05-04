package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalTabbedPaneUI

fun makeUI(): Component {
  val tabbedPane1 = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      if (getUI() is WindowsTabbedPaneUI) {
        setUI(object : WindowsTabbedPaneUI() {
          override fun shouldPadTabRun(tabPlacement: Int, run: Int) = false
        })
      } else {
        setUI(object : MetalTabbedPaneUI() {
          override fun shouldPadTabRun(tabPlacement: Int, run: Int) = false
        })
      }
    }
  }
  val tabbedPane2 = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      if (getUI() is WindowsTabbedPaneUI) {
        setUI(object : WindowsTabbedPaneUI() {
          override fun shouldPadTabRun(tabPlacement: Int, run: Int) = true
        })
      } else {
        setUI(object : MetalTabbedPaneUI() {
          override fun shouldPadTabRun(tabPlacement: Int, run: Int) = true
        })
      }
    }
  }

  return JPanel(GridLayout(0, 1, 2, 2)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeTabbedPane("default", JTabbedPane()))
    it.add(makeTabbedPane("shouldPadTabRun: false", tabbedPane1))
    it.add(makeTabbedPane("shouldPadTabRun: true", tabbedPane2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(title: String, tabbedPane: JTabbedPane): JTabbedPane {
  tabbedPane.addTab("1111111111111111111111111111", ColorIcon(Color.RED), JLabel(title))
  tabbedPane.addTab("2", ColorIcon(Color.GREEN), JLabel())
  tabbedPane.addTab("333333333333333333333333333333333", ColorIcon(Color.BLUE), JLabel())
  tabbedPane.addTab("444444444444", ColorIcon(Color.ORANGE), JLabel())
  return tabbedPane
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener {
      val m = lafRadioGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
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
