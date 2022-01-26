package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalTabbedPaneUI

fun makeUI(): Component {
  UIManager.put("TabbedPane.tabRunOverlay", 0)
  UIManager.put("TabbedPane.selectedLabelShift", 0)
  UIManager.put("TabbedPane.labelShift", 0)
  UIManager.put("TabbedPane.selectedTabPadInsets", Insets(0, 0, 0, 0))

  val tabbedPane = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsTabbedPaneUI) {
        object : WindowsTabbedPaneUI() {
          override fun shouldRotateTabRuns(tabPlacement: Int) = false
        }
      } else {
        object : MetalTabbedPaneUI() {
          override fun shouldRotateTabRuns(tabPlacement: Int) = false

          override fun shouldRotateTabRuns(tabPlacement: Int, selectedRun: Int) = false
        }
      }
      setUI(tmp)
    }
  }

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }

    it.add(makeTabbedPane(JTabbedPane()))
    it.add(makeTabbedPane(tabbedPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(tabbedPane: JTabbedPane) = tabbedPane.also {
  it.addTab("1111111111111111111111111111", ColorIcon(Color.RED), JLabel())
  it.addTab("2", ColorIcon(Color.GREEN), JLabel())
  it.addTab("333333333333333333333333333333333", ColorIcon(Color.BLUE), JLabel())
  it.addTab("444444444444", ColorIcon(Color.ORANGE), JLabel())
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
