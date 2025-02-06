package example

import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.swing.*

fun makeUI(): Component {
  val b1 = JButton("button")

  val icon = UIManager.getIcon("InternalFrame.icon")
  val b2 = JButton(icon)
  // b2.icon = icon
  b2.rolloverIcon = makeRolloverIcon(icon)

  val list = listOf(b1, b2)
  val p = JPanel()
  p.border = BorderFactory.createEmptyBorder(20, 10, 20, 10)
  list.forEach { p.add(it) }

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
  listOf(
    focusPainted,
    borderPainted,
    contentAreaFilled,
    rolloverEnabled,
  ).forEach { box.add(it) }

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRolloverIcon(srcIcon: Icon): Icon {
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
