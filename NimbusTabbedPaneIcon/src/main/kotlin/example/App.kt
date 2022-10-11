package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane() {
    override fun insertTab(title: String?, icon: Icon?, c: Component?, tip: String?, index: Int) {
      super.insertTab(title, icon, c, tip, index)
      val label = JLabel(title, icon, LEADING)
      setTabComponentAt(tabCount - 1, label)
    }
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  return JPanel(GridLayout(2, 1)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeTitledPanel("Default addTab(title, icon, c)", initTabbedPane(JTabbedPane())))
    it.add(makeTitledPanel("TabComponent + JLabel + LEADING", initTabbedPane(tabbedPane)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initTabbedPane(tabs: JTabbedPane): JTabbedPane {
  tabs.addTab("JTree", ColorIcon(Color.RED), JScrollPane(JTree()))
  tabs.addTab("JTable", ColorIcon(Color.GREEN), JScrollPane(JTable(8, 3)))
  tabs.addTab("JTextArea", ColorIcon(Color.BLUE), JScrollPane(JTextArea()))
  // tabs.addTab("JSplitPane", ColorIcon(Color.ORANGE), JSplitPane())
  // tabs.addTab("JLabel", ColorIcon(Color.CYAN), JLabel("JLabel"))
  return tabs
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
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
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
