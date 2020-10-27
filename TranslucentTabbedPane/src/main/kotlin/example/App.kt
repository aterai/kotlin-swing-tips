package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val bgc = Color(110, 110, 0, 100)
  val fgc = Color(255, 255, 0, 100)
  UIManager.put("TabbedPane.shadow", fgc)
  UIManager.put("TabbedPane.darkShadow", fgc)
  UIManager.put("TabbedPane.light", fgc)
  UIManager.put("TabbedPane.highlight", fgc)
  UIManager.put("TabbedPane.tabAreaBackground", fgc)
  UIManager.put("TabbedPane.unselectedBackground", fgc)
  UIManager.put("TabbedPane.background", bgc)
  UIManager.put("TabbedPane.foreground", Color.WHITE)
  UIManager.put("TabbedPane.focus", fgc)
  UIManager.put("TabbedPane.contentAreaColor", fgc)
  UIManager.put("TabbedPane.selected", fgc)
  UIManager.put("TabbedPane.selectHighlight", fgc)

  // UIManager.put("TabbedPane.borderHighlightColor", fgc); // Do not work
  // Maybe "TabbedPane.borderHightlightColor" is a typo,
  // but this is defined in MetalTabbedPaneUI
  UIManager.put("TabbedPane.borderHightlightColor", fgc)
  val tab1panel = JPanel()
  tab1panel.background = Color(0, 220, 220, 50)

  val tab2panel = JPanel()
  tab2panel.background = Color(220, 0, 0, 50)

  val tab3panel = JPanel()
  tab3panel.background = Color(0, 0, 220, 50)

  val cb = JCheckBox("setOpaque(false)")
  cb.isOpaque = false
  cb.foreground = Color.WHITE
  tab3panel.add(cb)
  tab3panel.add(JCheckBox("setOpaque(true)"))

  val tabs = JTabbedPane()
  tabs.addTab("Tab 1", tab1panel)
  tabs.addTab("Tab 2", tab2panel)
  tabs.addTab("Tab 3", AlphaContainer(tab3panel))

  val cl = Thread.currentThread().contextClassLoader
  val img = ImageIcon(cl.getResource("example/test.png")).image
  val p = object : JPanel(BorderLayout()) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.drawImage(img, 0, 0, width, height, this)
    }
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())
  EventQueue.invokeLater { p.rootPane.jMenuBar = mb }

  p.add(tabs)
  p.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
  p.preferredSize = Dimension(320, 240)
  return p
}

private class AlphaContainer(private val component: JComponent) : JPanel(BorderLayout()) {
  init {
    component.isOpaque = false
    add(component)
  }

  override fun isOpaque() = false

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    g.color = component.background
    g.fillRect(0, 0, width, height)
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
