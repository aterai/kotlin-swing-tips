package example

import java.awt.*
import javax.swing.*

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
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

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

  // ---- 0 ----
  val cb0 = list[0]
  cb0.isEditable = false
  cb0.toolTipText = "cb.setEditable(false);"

  // ---- 1 ----
  val cb1 = list[1]
  cb1.isEditable = true
  (cb1.editor.editorComponent as? JTextField)?.also {
    it.border = BorderFactory.createCompoundBorder(it.border, getPaddingBorder(isColor))
  }
  cb1.toolTipText = "ed.setBorder(BorderFactory.createCompoundBorder(ed.getBorder(), padding));"

  // ---- 2 ----
  val cb2 = list[2]
  cb2.isEditable = true
  (cb2.editor.editorComponent as? JTextField)?.also {
    it.border = getPaddingBorder(isColor)
  }
  cb2.toolTipText = "ed.setBorder(padding);"

  // ---- 3 ----
  val cb3 = list[3]
  cb3.isEditable = true
  (cb3.editor.editorComponent as? JTextField)?.also {
    val i = it.insets
    it.margin = Insets(i.top, i.left + 5, i.bottom, i.right)
  }
  cb3.toolTipText = "Insets i = ed.getInsets(); ed.setMargin(new Insets(i.top, i.left + 5, ...));"

  // ---- 4 ----
  val cb4 = list[4]
  cb4.isEditable = true
  (cb4.editor.editorComponent as? JTextField)?.also {
    val m = it.margin
    it.margin = Insets(m.top, m.left + 5, m.bottom, m.right)
  }
  cb4.toolTipText = "Insets m = ed.getMargin(); ed.setMargin(new Insets(m.top, m.left + 5, ...));"

  // ---- 5 ----
  val cb5 = list[5]
  cb5.isEditable = true
  cb5.border = BorderFactory.createCompoundBorder(cb5.border, getPaddingBorder(isColor))
  cb5.toolTipText = "cb.setBorder(BorderFactory.createCompoundBorder(cb.getBorder(), padding));"

  // ---- 6 ----
  val cb6 = list[6]
  cb6.isEditable = true
  cb6.border = BorderFactory.createCompoundBorder(getPaddingBorder(isColor), cb6.border)
  cb6.toolTipText = "cb.setBorder(BorderFactory.createCompoundBorder(cb, cb.getBorder()));"
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
