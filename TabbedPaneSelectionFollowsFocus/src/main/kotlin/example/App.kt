package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.plaf.metal.MetalTabbedPaneUI

private var focusIdx = -1

fun makeUI(): Component {
  val tabs = object : JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT) {
    override fun updateUI() {
      super.updateUI()
      if (getUI() is MetalTabbedPaneUI) {
        setUI(object : MetalTabbedPaneUI() {
          override fun navigateSelectedTab(direction: Int) {
            super.navigateSelectedTab(direction)
            focusIdx = focusIndex
          }
        })
      }
    }
  }
  tabs.addChangeListener {
    focusIdx = tabs.selectedIndex
    tabs.repaint()
  }
  val help = """
    SPACE:	selectTabWithFocus
    LEFT:	navigateLeft
    RIGHT:	navigateRight
  """.trimIndent()
  val textArea = JTextArea(help)
  textArea.isEditable = false
  tabs.addTab("help", JScrollPane(textArea))
  for (i in 0..10) {
    tabs.addTab("title$i", JLabel("JLabel$i"))
  }
  val im = tabs.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "selectTabWithFocus")

  val key = "TabbedPane.selectionFollowsFocus"
  val check = object : JCheckBox(key, UIManager.getBoolean(key)) {
    override fun updateUI() {
      super.updateUI()
      val b = UIManager.getLookAndFeelDefaults().getBoolean(key)
      isSelected = b
      UIManager.put(key, b)
    }
  }
  check.isFocusable = false
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    UIManager.put(key, b)
    SwingUtilities.updateComponentTreeUI(tabs)
  }
  val layerUI = object : LayerUI<JTabbedPane>() {
    override fun paint(g: Graphics, c: JComponent) {
      super.paint(g, c)
      if (c is JLayer<*>) {
        val tabbedPane = c.view as JTabbedPane
        if (focusIdx >= 0 && focusIdx != tabbedPane.selectedIndex) {
          val r = tabbedPane.getBoundsAt(focusIdx)
          val g2 = g.create() as? Graphics2D ?: return
          g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
          g2.paint = Color.RED
          g2.fill(r)
          g2.dispose()
        }
      }
    }
  }

  return JPanel(BorderLayout(5, 5)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    mb.add(check)
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(tabs, layerUI))
    it.preferredSize = Dimension(320, 240)
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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
