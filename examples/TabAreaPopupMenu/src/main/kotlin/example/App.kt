package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

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
      val contains = getTabAreaBounds(this).contains(e.point)
      componentPopupMenu = if (selected && contains) popup2 else popup1
      return super.getPopupLocation(e)
    }
  }
  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  tabbedPane.addTab("Title: 0", JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(tabbedPane, makeTestLayer()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getTabAreaBounds(tabbedPane: JTabbedPane): Rectangle {
  val r = SwingUtilities.calculateInnerArea(tabbedPane, null)
  val cr = tabbedPane.selectedComponent?.bounds ?: Rectangle()
  val tp = tabbedPane.tabPlacement
  // Note: BasicTabbedPaneUI#getTabAreaInsets() causes rotation
  val i1 = UIManager.getInsets("TabbedPane.tabAreaInsets")
  val i2 = UIManager.getInsets("TabbedPane.contentBorderInsets")
  if (tp == SwingConstants.TOP || tp == SwingConstants.BOTTOM) {
    r.height -= cr.height + i1.top + i1.bottom + i2.top + i2.bottom
    // r.x += i1.left
    r.y += if (tp == SwingConstants.TOP) {
      i1.top
    } else {
      cr.y + cr.height + i1.bottom + i2.bottom
    }
  } else {
    r.width -= cr.width + i1.top + i1.bottom + i2.left + i2.right
    r.x += if (tp == SwingConstants.LEFT) {
      i1.top
    } else {
      cr.x + cr.width + i1.bottom + i2.right
    }
    // r.y += i1.left
  }
  return r
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
    (popup.invoker as? JTabbedPane)?.also { tabbedPane ->
      val name = tabbedPane.getTitleAt(tabbedPane.selectedIndex)
      val textField = JTextField(name)
      val result = JOptionPane.showConfirmDialog(
        tabbedPane,
        textField,
        rename.text,
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE,
      )
      if (result == JOptionPane.OK_OPTION) {
        val str = textField.text.trim { it <= ' ' }
        if (str != name) {
          tabbedPane.setTitleAt(tabbedPane.selectedIndex, str)
        }
      }
    }
  }
  popup.addSeparator()
  val close = popup.add("Close")
  close.accelerator = KeyStroke.getKeyStroke("ctrl F4")
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
  TabPlacement.entries.forEach {
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

private fun makeTestLayer(): LayerUI<JTabbedPane> {
  return object : LayerUI<JTabbedPane>() {
    override fun paint(g: Graphics, c: JComponent) {
      super.paint(g, c)
      if (c is JLayer<*>) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = Color.RED
        (c.view as? JTabbedPane)?.also {
          val r = getTabAreaBounds(it)
          g2.drawRect(r.x, r.y, r.width - 1, r.height - 1)
        }
        g2.dispose()
      }
    }
  }
}

private enum class TabPlacement(
  val placement: Int,
) {
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
