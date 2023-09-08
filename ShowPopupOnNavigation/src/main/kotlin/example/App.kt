package example

import java.awt.*
import javax.swing.*

private const val SHOW_POPUP_NAVI = "ComboBox.showPopupOnNavigation"

fun makeUI(): Component {
  val help = JLabel("This setting only responds to the upwards arrow key↑")
  help.alignmentX = Component.LEFT_ALIGNMENT

  val check1 = JCheckBox(SHOW_POPUP_NAVI)
  check1.isFocusable = false
  check1.addActionListener { e ->
    UIManager.put(SHOW_POPUP_NAVI, (e.source as? JCheckBox)?.isSelected == true)
  }

  val combo = object : JComboBox<String>(makeModel()) {
    override fun updateUI() {
      super.updateUI()
      val flg = UIManager.getLookAndFeelDefaults().getBoolean(SHOW_POPUP_NAVI)
      UIManager.put(SHOW_POPUP_NAVI, flg)
      check1.isSelected = flg
    }
  }
  combo.selectedIndex = 5
  combo.alignmentX = Component.LEFT_ALIGNMENT

  val check2 = JCheckBox("isEditable")
  check2.isFocusable = false
  check2.addActionListener { e ->
    combo.isEditable = (e.source as? JCheckBox)?.isSelected == true
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { combo.rootPane.jMenuBar = mb }

  val box = Box.createVerticalBox().also {
    it.add(help)
    it.add(Box.createVerticalStrut(5))
    it.add(check1)
    it.add(Box.createVerticalStrut(5))
    it.add(check2)
    it.add(Box.createVerticalStrut(15))
    it.add(combo)
    it.border = BorderFactory.createEmptyBorder(5, 2, 5, 2)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  for (i in 0 until 10) {
    model.addElement("item: $i")
  }
  return model
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
