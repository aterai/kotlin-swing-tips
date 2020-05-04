package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val box1 = Box.createVerticalBox()
  box1.border = BorderFactory.createTitledBorder("setBorderPainted: false")

  val c0 = JCheckBox("setBorderPaintedFlat: false")
  c0.isBorderPainted = false
  c0.isBorderPaintedFlat = false
  box1.add(c0)
  box1.add(Box.createVerticalStrut(5))

  val c1 = JCheckBox("setBorderPaintedFlat: true")
  c1.isBorderPainted = false
  c1.isBorderPaintedFlat = true
  box1.add(c1)

  val box2 = Box.createVerticalBox()
  box2.border = BorderFactory.createTitledBorder("setBorderPainted: true")

  val c2 = JCheckBox("setBorderPaintedFlat: true")
  c2.isBorderPainted = true
  c2.isBorderPaintedFlat = true
  box2.add(c2)
  box2.add(Box.createVerticalStrut(5))

  val c3 = JCheckBox("setBorderPaintedFlat: false")
  c3.isBorderPainted = true
  c3.isBorderPaintedFlat = false
  box2.add(c3)
  box2.add(Box.createVerticalStrut(5))

  val box = Box.createVerticalBox()
  box.add(box1)
  box.add(Box.createVerticalStrut(10))
  box.add(box2)

  return JPanel(BorderLayout(5, 5)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
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
