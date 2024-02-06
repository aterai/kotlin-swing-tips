package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane() {
    private val popup1 = makeTabPopupMenu()
    private val popup2 = makeTabAreaPopupMenu()

    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        SwingUtilities.updateComponentTreeUI(popup1)
        SwingUtilities.updateComponentTreeUI(popup2)
        componentPopupMenu = popup1
      }
    }

    override fun getPopupLocation(e: MouseEvent): Point? {
      val selected = indexAtLocation(e.x, e.y) < 0
      val contains = getTabAreaBounds().contains(e.point)
      componentPopupMenu = if (selected && contains) popup2 else popup1
      return super.getPopupLocation(e)
    }

    private fun getTabAreaBounds(): Rectangle {
      val tabbedRect = bounds
      val compRect = selectedComponent?.let { it.bounds } ?: Rectangle()
      val tabPlacement = getTabPlacement()
      if (isTopBottomTabPlacement(tabPlacement)) {
        tabbedRect.height = tabbedRect.height - compRect.height
        if (tabPlacement == BOTTOM) {
          tabbedRect.y += compRect.y + compRect.height
        }
      } else {
        tabbedRect.width = tabbedRect.width - compRect.width
        if (tabPlacement == RIGHT) {
          tabbedRect.x += compRect.x + compRect.width
        }
      }
      return tabbedRect
    }

    private fun isTopBottomTabPlacement(tabPlacement: Int) =
      tabPlacement == TOP || tabPlacement == BOTTOM
  }
  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  tabbedPane.addTab("Title: 0", JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabPopupMenu(): JPopupMenu {
  val popup = JPopupMenu()
  popup.add("New tab").addActionListener {
    (popup.invoker as? JTabbedPane)?.also {
      val title = "Title: " + it.tabCount
      it.addTab(title, JScrollPane(JTextArea()))
      it.selectedIndex = it.tabCount - 1
    }
  }
  popup.addSeparator()
  val rename = popup.add("Rename")
  rename.addActionListener {
    (popup.invoker as? JTabbedPane)?.also {
      val name = it.getTitleAt(it.selectedIndex)
      val textField = JTextField(name)
      val result = JOptionPane.showConfirmDialog(
        it,
        textField,
        rename.text,
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE,
      )
      if (result == JOptionPane.OK_OPTION) {
        val str = textField.text.trim { it <= ' ' }
        if (str != name) {
          it.setTitleAt(it.selectedIndex, str)
        }
      }
    }
  }
  popup.addSeparator()
  val close = popup.add("Close")
  close.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.CTRL_DOWN_MASK)
  close.addActionListener {
    (popup.invoker as? JTabbedPane)?.also {
      it.remove(it.selectedIndex)
    }
  }
  val closeAll = popup.add("Close all")
  closeAll.addActionListener {
    (popup.invoker as? JTabbedPane)?.removeAll()
  }
  val closeAllButActive = popup.add("Close all bat active")
  closeAllButActive.addActionListener {
    (popup.invoker as? JTabbedPane)?.also {
      val idx = it.selectedIndex
      val title = it.getTitleAt(idx)
      val cmp = it.getComponentAt(idx)
      it.removeAll()
      it.addTab(title, cmp)
    }
  }
  return popup
}

private fun makeTabAreaPopupMenu(): JPopupMenu {
  val popup = JPopupMenu()
  popup.add("New tab").addActionListener {
    (popup.invoker as? JTabbedPane)?.also {
      val title = "Title: " + it.tabCount
      it.addTab(title, JScrollPane(JTextArea()))
      it.selectedIndex = it.tabCount - 1
    }
  }
  popup.addSeparator()
  val group = ButtonGroup()
  val handler = ItemListener {
    val c = popup.invoker
    if (it.stateChange == ItemEvent.SELECTED && c is JTabbedPane) {
      val m = group.selection
      val tp = TabPlacement.valueOf(m.actionCommand)
      c.tabPlacement = tp.placement
    }
  }
  TabPlacement.values().forEach {
    val name = it.name
    val selected = it == TabPlacement.TOP
    val item = JRadioButtonMenuItem(name, selected)
    item.addItemListener(handler)
    item.actionCommand = name
    popup.add(item)
    group.add(item)
  }
  return popup
}

private enum class TabPlacement(val placement: Int) {
  TOP(SwingConstants.TOP),
  LEFT(SwingConstants.LEFT),
  BOTTOM(SwingConstants.BOTTOM),
  RIGHT(SwingConstants.RIGHT),
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
