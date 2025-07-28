package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val popup0 = JPopupMenu("Default JPopupMenu")
  val tree0 = JTree()
  tree0.setComponentPopupMenu(initPopup(popup0))

  val popup1 = object : JPopupMenu("JPopupMenu#setLabel(...)") {
    override fun updateUI() {
      setBorder(null)
      super.updateUI()
      val border = getBorder()
      val title = label
      if (!isCompoundMotifBorderBorder(border) && title != null) {
        val color = UIManager.getColor("Separator.foreground")
        val labelBorder = BorderFactory.createTitledBorder(
          BorderFactory.createMatteBorder(1, 0, 0, 0, color),
          title,
          TitledBorder.CENTER,
          TitledBorder.ABOVE_TOP,
          getFont(),
          getForeground(),
        )
        setBorder(BorderFactory.createCompoundBorder(border, labelBorder))
      }
    }
  }
  val tree1 = JTree()
  tree1.setComponentPopupMenu(initPopup(popup1))
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(1, 2)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(tree0))
    it.add(JScrollPane(tree1))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun isCompoundMotifBorderBorder(b: Border) = if (b is CompoundBorder) {
  isMotifBorder(b.getInsideBorder()) || isMotifBorder(b.getOutsideBorder())
} else {
  false
}

private fun isMotifBorder(b: Border) = b.javaClass.name.contains("MotifBorders")

private fun initPopup(popup: JPopupMenu): JPopupMenu {
  popup.add("JMenuItem1")
  popup.add("JMenuItem2")
  popup.addSeparator()
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  val menu = JMenu("JMenu")
  menu.add("Sub JMenuItem 1")
  menu.add("Sub JMenuItem 2")
  popup.add(menu)
  return popup
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
