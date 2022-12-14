package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val combo = makeComboBox()
  UIManager.put("ComboBox.font", combo.font)

  val check = JCheckBox("<html>addAuxiliaryLookAndFeel<br>(Disable Right Click)")

  val auxLookAndFeel = AuxiliaryWindowsLookAndFeel()
  UIManager.addPropertyChangeListener { e ->
    if ("lookAndFeel" == e.propertyName) {
      val lnf = e.newValue.toString()
      if (lnf.contains("Windows")) {
        if (check.isSelected) {
          UIManager.addAuxiliaryLookAndFeel(auxLookAndFeel)
        }
        check.isEnabled = true
      } else {
        UIManager.removeAuxiliaryLookAndFeel(auxLookAndFeel)
        check.isEnabled = false
      }
    }
  }
  check.addActionListener { e ->
    val lnf = UIManager.getLookAndFeel().name
    if ((e.source as? JCheckBox)?.isSelected == true && lnf.contains("Windows")) {
      UIManager.addAuxiliaryLookAndFeel(auxLookAndFeel)
    } else {
      UIManager.removeAuxiliaryLookAndFeel(auxLookAndFeel)
    }
    SwingUtilities.updateComponentTreeUI(check.rootPane)
  }

  combo.isEditable = true

  val box = Box.createVerticalBox()
  box.add(check)
  box.add(Box.createVerticalStrut(5))
  box.add(combo)
  box.add(Box.createVerticalStrut(5))
  box.add(makeComboBox())
  box.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComboBox(): JComboBox<String> {
  val model = DefaultComboBoxModel<String>().also {
    it.addElement("aaa aaa")
    it.addElement("aaa ab bb")
    it.addElement("aaa ab bb cc")
    it.addElement("1354123451234513512")
    it.addElement("bbb1")
    it.addElement("bbb12")
  }
  return JComboBox(model)
}

private object LookAndFeelUtil {
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
