package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
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

  // UIManager.put("TabbedPane.borderHighlightColor", fgc) // Do not work
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

  val url = Thread.currentThread().contextClassLoader.getResource("example/test.png")
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  // val img = ImageIcon(cl.getResource("example/test.png")).image
  val p = object : JPanel(BorderLayout()) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.drawImage(img, 0, 0, width, height, this)
    }
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { p.rootPane.jMenuBar = mb }

  p.add(tabs)
  p.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
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

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.GRAY
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
