package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel()
  val b1 = JButton("button")
  val b2 = JButton()
  val cl = Thread.currentThread().contextClassLoader
  val rss = ImageIcon(cl.getResource("example/feed-icon-14x14.png")) // http://feedicons.com/
  b2.icon = rss
  b2.rolloverIcon = makeRolloverIcon(rss)
  p.add(b1)
  p.add(b2)
  p.border = BorderFactory.createEmptyBorder(20, 10, 20, 10)
  val list = listOf(b1, b2)

  val focusPainted = JCheckBox("setFocusPainted", true)
  focusPainted.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    list.forEach { it.isFocusPainted = flg }
    p.revalidate()
  }

  val borderPainted = JCheckBox("setBorderPainted", true)
  borderPainted.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    list.forEach { it.isBorderPainted = flg }
    p.revalidate()
  }

  val contentAreaFilled = JCheckBox("setContentAreaFilled", true)
  contentAreaFilled.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    list.forEach { it.isContentAreaFilled = flg }
    p.revalidate()
  }

  val rolloverEnabled = JCheckBox("setRolloverEnabled", true)
  rolloverEnabled.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    list.forEach { it.isRolloverEnabled = flg }
    p.revalidate()
  }

  val box = Box.createVerticalBox()
  listOf(focusPainted, borderPainted, contentAreaFilled, rolloverEnabled).forEach { box.add(it) }

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

  private fun makeRolloverIcon(srcIcon: ImageIcon): ImageIcon {
    val w = srcIcon.iconWidth
    val h = srcIcon.iconHeight
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    srcIcon.paintIcon(null, g2, 0, 0)
    val scaleFactors = floatArrayOf(1.2f, 1.2f, 1.2f, 1f)
    val offsets = floatArrayOf(0f, 0f, 0f, 0f)
    val op = RescaleOp(scaleFactors, offsets, g2.renderingHints)
    g2.dispose()
    return ImageIcon(op.filter(img, null))
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
