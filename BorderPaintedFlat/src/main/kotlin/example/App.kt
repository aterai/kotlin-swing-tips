package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout(5, 5)) {
  init {
    val box1 = Box.createVerticalBox()
    box1.setBorder(BorderFactory.createTitledBorder("setBorderPainted: false"))

    val c0 = JCheckBox("setBorderPaintedFlat: false")
    c0.setBorderPainted(false)
    c0.setBorderPaintedFlat(false)
    box1.add(c0)
    box1.add(Box.createVerticalStrut(5))

    val c1 = JCheckBox("setBorderPaintedFlat: true")
    c1.setBorderPainted(false)
    c1.setBorderPaintedFlat(true)
    box1.add(c1)

    val box2 = Box.createVerticalBox()
    box2.setBorder(BorderFactory.createTitledBorder("setBorderPainted: true"))

    val c2 = JCheckBox("setBorderPaintedFlat: true")
    c2.setBorderPainted(true)
    c2.setBorderPaintedFlat(true)
    box2.add(c2)
    box2.add(Box.createVerticalStrut(5))

    val c3 = JCheckBox("setBorderPaintedFlat: false")
    c3.setBorderPainted(true)
    c3.setBorderPaintedFlat(false)
    box2.add(c3)
    box2.add(Box.createVerticalStrut(5))

    val box = Box.createVerticalBox()
    box.add(box1)
    box.add(Box.createVerticalStrut(10))
    box.add(box2)
    add(box, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
internal object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafRadioGroup.getSelection()
      runCatching {
        setLookAndFeel(m.getActionCommand())
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
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel)
    }
  }

  private fun updateLookAndFeel() {
    for (window in Frame.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      val mb = JMenuBar()
      mb.add(LookAndFeelUtil.createLookAndFeelMenu())
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      setJMenuBar(mb)
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
