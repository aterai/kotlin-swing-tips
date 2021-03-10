package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private val panel = JPanel()
private val check = JCheckBox("color")

fun makeUI(): Component {
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  check.addActionListener {
    layoutComboBoxPanel(panel, initComboBoxes(check.isSelected))
    panel.revalidate()
  }
  layoutComboBoxPanel(panel, initComboBoxes(check.isSelected))

  val box = Box.createHorizontalBox()
  box.add(check)
  box.add(Box.createHorizontalGlue())

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(panel)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun layoutComboBoxPanel(p2: JPanel, list: List<JComboBox<*>>) {
  p2.removeAll()
  p2.layout = GridBagLayout()
  val inside = BorderFactory.createEmptyBorder(10, 5 + 2, 10, 10 + 2)
  val outside = BorderFactory.createTitledBorder("JComboBox Padding Test")
  p2.border = BorderFactory.createCompoundBorder(outside, inside)
  val c = GridBagConstraints()
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.LINE_END
  for (i in list.indices) {
    c.gridx = 0
    c.weightx = 0.0
    c.fill = GridBagConstraints.NONE
    p2.add(makeLabel(i), c)
    c.gridx = 1
    c.weightx = 1.0
    c.fill = GridBagConstraints.HORIZONTAL
    p2.add(list[i], c)
  }
  p2.revalidate() // ??? JDK 1.7.0 Nimbus ???
}

private fun initComboBoxes(isColor: Boolean): List<JComboBox<*>> {
  val list = mutableListOf<JComboBox<*>>()
  for (i in 0..6) {
    list.add(makeComboBox())
  }

  // ---- 00 ----
  val combo00 = list[0]
  combo00.isEditable = false
  combo00.toolTipText = "combo.setEditable(false);"

  // ---- 01 ----
  val combo01 = list[1]
  combo01.isEditable = true
  (combo01.editor.editorComponent as? JTextField)?.also {
    it.border = BorderFactory.createCompoundBorder(it.border, getPaddingBorder(isColor))
  }
  combo01.toolTipText = "editor.setBorder(BorderFactory.createCompoundBorder(editor.getBorder(), padding));"

  // ---- 02 ----
  val combo02 = list[2]
  combo02.isEditable = true
  (combo02.editor.editorComponent as? JTextField)?.also {
    it.border = getPaddingBorder(isColor)
  }
  combo02.toolTipText = "editor.setBorder(padding);"

  // ---- 03 ----
  val combo03 = list[3]
  combo03.isEditable = true
  (combo03.editor.editorComponent as? JTextField)?.also {
    val i = it.insets
    it.margin = Insets(i.top, i.left + 5, i.bottom, i.right)
  }
  combo03.toolTipText = "Insets i = editor.getInsets(); editor.setMargin(new Insets(i.top, i.left + 5, ...));"

  // ---- 04 ----
  val combo04 = list[4]
  combo04.isEditable = true
  (combo04.editor.editorComponent as? JTextField)?.also {
    val m = it.margin
    it.margin = Insets(m.top, m.left + 5, m.bottom, m.right)
  }
  combo04.toolTipText = "Insets m = editor.getMargin(); editor.setMargin(new Insets(m.top, m.left + 5, ...));"

  // ---- 05 ----
  val combo05 = list[5]
  combo05.isEditable = true
  combo05.border = BorderFactory.createCompoundBorder(combo05.border, getPaddingBorder(isColor))
  combo05.toolTipText = "combo.setBorder(BorderFactory.createCompoundBorder(combo.getBorder(), padding));"

  // ---- 06 ----
  val combo06 = list[6]
  combo06.isEditable = true
  combo06.border = BorderFactory.createCompoundBorder(getPaddingBorder(isColor), combo06.border)
  combo06.toolTipText = "combo.setBorder(BorderFactory.createCompoundBorder(padding, combo.getBorder()));"
  if (isColor) {
    val c = Color(.8f, 1f, .8f)
    for (cb in list) {
      cb.isOpaque = true
      cb.background = c
      (cb.editor.editorComponent as? JTextField)?.also {
        it.isOpaque = true
        it.background = c
      }
    }
  }
  return list
}

private fun makeLabel(num: Int) = JLabel("$num:")

private fun getPaddingBorder(isColor: Boolean) = if (isColor) {
  BorderFactory.createMatteBorder(0, 5, 0, 0, Color(1f, .8f, .8f, .5f))
} else {
  BorderFactory.createEmptyBorder(0, 5, 0, 0)
}

private fun makeComboBox(): JComboBox<String> {
  val model = DefaultComboBoxModel<String>().also {
    it.addElement("11111111111111111111111111111")
    it.addElement("222222222222")
    it.addElement("3333333333")
    it.addElement("444444")
    it.addElement("555")
  }
  return object : JComboBox<String>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val lcr = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        lcr.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          (it as? JComponent)?.border = getPaddingBorder(false)
        }
      }
    }
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
