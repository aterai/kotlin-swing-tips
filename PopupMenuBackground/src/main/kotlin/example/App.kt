package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.BorderUIResource
import javax.swing.plaf.basic.BasicPopupMenuUI
import javax.swing.tree.DefaultTreeCellRenderer

fun makeUI(): Component {
  val tree0 = JTree()
  tree0.componentPopupMenu = initPopupMenu(JPopupMenu())

  val tree1 = makeTree()
  tree1.componentPopupMenu = initPopupMenu(makePopupMenu())

  val tree2 = makeTree()
  tree2.componentPopupMenu = initPopupMenu(DarkModePopupMenu())

  val tabs = JTabbedPane()
  tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  tabs.addTab("Default", JScrollPane(tree0))
  tabs.addTab("setBackground(...)", JScrollPane(tree1))
  tabs.addTab("BasicPopupMenuUI", JScrollPane(tree2))
  tabs.selectedIndex = tabs.tabCount - 1

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTree() = object : JTree() {
  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    val r = DefaultTreeCellRenderer()
    r.textNonSelectionColor = Color.WHITE
    r.backgroundNonSelectionColor = Color.DARK_GRAY
    setCellRenderer(r)
    background = Color.DARK_GRAY
    foreground = Color.WHITE
  }
}

private fun makePopupMenu() = object : JPopupMenu() {
  override fun updateUI() {
    super.updateUI()
    background = Color.DARK_GRAY
    border = BorderUIResource(BorderFactory.createLineBorder(Color.LIGHT_GRAY))
    EventQueue.invokeLater {
      for (m in subElements) {
        val c = m.component
        c.foreground = Color.WHITE
        (c as? JComponent)?.isOpaque = false
      }
    }
  }
}

private fun initPopupMenu(popup: JPopupMenu): JPopupMenu {
  popup.add("Cut")
  popup.add("Copy")
  popup.add("Paste")
  popup.add("Delete")
  popup.addSeparator()
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  return popup
}

private class DarkModePopupMenu : JPopupMenu() {
  override fun updateUI() {
    super.updateUI()
    setUI(BasicPopupMenuUI())
    background = Color.DARK_GRAY
    border = BorderUIResource(BorderFactory.createLineBorder(Color.LIGHT_GRAY))
    EventQueue.invokeLater {
      for (m in subElements) {
        val c = m.component
        c.foreground = Color.WHITE
        (c as? JComponent)?.isOpaque = false
      }
    }
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

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
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
