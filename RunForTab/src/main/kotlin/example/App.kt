package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.*

private fun makeUI(): Component {
  val tabs = object : JTabbedPane() {
    override fun addTab(title: String, icon: Icon, component: Component) {
      super.addTab(title, icon, component, title)
    }

    override fun getToolTipText(e: MouseEvent): String? {
      var tip = super.getToolTipText(e)
      val idx = indexAtLocation(e.x, e.y)
      if (idx >= 0 && isHorizontalTabPlacement()) {
        val run = getRunForTab(tabCount, idx)
        tip = "%s: Run: %d".format(tip, run)
      }
      return tip
    }

    private fun getRunForTab(tabCount: Int, tabIndex: Int): Int {
      val runCount = tabRunCount
      val taRect = getTabAreaRect(tabCount)
      val runHeight = taRect.height / runCount
      val tabRect = getBoundsAt(tabIndex)
      val pt = Point2D.Double(tabRect.centerX, tabRect.centerY)
      val runRect = Rectangle(taRect.x, taRect.y, taRect.width, runHeight)
      var run = -1
      for (i in 0 until runCount) {
        if (runRect.contains(pt)) {
          run = i
        }
        runRect.translate(0, runHeight)
      }
      return if (getTabPlacement() == TOP) runCount - run - 1 else run
    }

    private fun getTabAreaRect(tabCount: Int): Rectangle {
      val rect = getBoundsAt(0)
      for (i in 0 until tabCount) {
        rect.add(getBoundsAt(i))
      }
      return rect
    }

    private fun isHorizontalTabPlacement(): Boolean {
      return getTabPlacement() == TOP || getTabPlacement() == BOTTOM
    }
  }
  tabs.addTab("111111111111111111111111", ColorIcon(Color.RED), JLabel())
  tabs.addTab("2", ColorIcon(Color.GREEN), JLabel())
  tabs.addTab("33333333333333333333333333333", ColorIcon(Color.BLUE), JLabel())
  tabs.addTab("444444444444", ColorIcon(Color.ORANGE), JLabel())
  tabs.addTab("55555555555555555", ColorIcon(Color.YELLOW), JLabel())

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(makeTabPlacementMenu(tabs))

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabPlacementMenu(tabs: JTabbedPane): JMenu {
  val group = ButtonGroup()
  val handler = ItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val m = group.selection
      val tp = TabPlacement.valueOf(m.actionCommand)
      tabs.tabPlacement = tp.placement
    }
  }
  val menu = JMenu("TabPlacement")
  TabPlacement.values().forEach { tp ->
    val item = JRadioButtonMenuItem(tp.name, tp == TabPlacement.TOP).also {
      it.addItemListener(handler)
      it.actionCommand = tp.name
    }
    menu.add(item)
    group.add(item)
  }
  return menu
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
