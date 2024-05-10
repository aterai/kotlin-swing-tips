package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.metal.MetalComboBoxUI

fun makeUI(): Component {
  val p = Box.createVerticalBox()
  val m = makeComboBoxModel()
  p.add(makeTitledPanel("Left clipped", LeftClippedComboBox<String>(m)))
  p.add(Box.createVerticalStrut(5))
  p.add(makeTitledPanel("Default", JComboBox<String>(m)))
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private class LeftClippedComboBox<E>(m: ComboBoxModel<E>) : JComboBox<E>(m) {
  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    setRenderer(makeComboBoxRenderer(this))
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder(title)
  box.add(Box.createVerticalStrut(2))
  box.add(c)
  return box
}

private fun makeComboBoxModel() = DefaultComboBoxModel<String>().also {
  it.addElement("1234567890123456789012/3456789012345678901234567890/12345678901234567890.jpg")
  it.addElement("abc.tif")
  it.addElement("""\0123456789\0123456789\0123456789.avi""")
  it.addElement("0123456789.pdf")
  it.addElement("c:/12312343245/643667345624523451/324513/41234125/134513451345135125.mpg")
  it.addElement("http://localhost/1234567890123456789/3456789012345/678901234567894567890.jpg")
}

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

private fun makeComboBoxRenderer(combo: JComboBox<*>) =
  object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean,
    ): Component {
      val c = super.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus,
      )
      if (c is JLabel) {
        val w = getAvailableWidth(combo, index, insets)
        val fm = c.getFontMetrics(c.font)
        c.text = if (fm.stringWidth(text) <= w) text else getLeftClippedText(text, fm, w)
      }
      return c
    }
  }

private fun getAvailableWidth(
  combo: JComboBox<*>,
  index: Int,
  rendererIns: Insets,
): Int {
  val arrowButton = descendants(combo)
    .filterIsInstance<JButton>()
    .firstOrNull()
  val r = SwingUtilities.calculateInnerArea(combo, null)
  var availableWidth = r.width - rendererIns.left - rendererIns.right
  availableWidth = getLookAndFeelDependWidth(combo, availableWidth)
  if (index < 0) {
    val buttonSize = arrowButton?.width
      ?: r.height - rendererIns.top - rendererIns.bottom
    availableWidth -= buttonSize
  }
  return availableWidth
}

private fun getLookAndFeelDependWidth(
  combo: JComboBox<*>,
  width: Int,
): Int {
  var availableWidth = width
  val padding = UIManager.getInsets("ComboBox.padding")
  if (padding != null) {
    // NimbusComboBoxUI only?
    availableWidth -= padding.left + padding.right
  }
  val ui = combo.ui
  if (ui is MetalComboBoxUI) {
    // Magic number in MetalComboBoxUI#paintCurrentValue(...)
    // This is really only called if we're using ocean.
    // if (MetalLookAndFeel.usingOcean()) {
    //   bounds.width -= 3
    availableWidth -= 3
  } else if (ui.javaClass.name.contains("Windows")) {
    // Magic number in WindowsComboBoxUI#paintCurrentValue(...)
    // XPStyle xp = XPStyle.getXP()
    // if (xp != null) {
    //   bounds.width -= 4
    // } else {
    //   bounds.width -= 2
    // }
    val lnfName = UIManager.getLookAndFeel().name
    availableWidth -= if (lnfName == "Windows") 4 else 2
  }
  return availableWidth
}

private fun getLeftClippedText(
  text: String,
  fm: FontMetrics,
  availableWidth: Int,
): String {
  val dots = "..."
  var textWidth = fm.stringWidth(dots)
  val len = text.length
  // @see Unicode surrogate programming with the Java language
  // https://www.ibm.com/developerworks/library/j-unicode/index.html
  // https://www.ibm.com/developerworks/jp/ysl/library/java/j-unicode_surrogate/index.html
  val acp = IntArray(text.codePointCount(0, len))
  var j = acp.size
  var i = len
  while (i > 0) {
    val cp = text.codePointBefore(i)
    textWidth += fm.charWidth(cp)
    if (textWidth > availableWidth) {
      break
    }
    acp[--j] = cp
    i = text.offsetByCodePoints(i, -1)
  }
  return dots + String(acp, j, acp.size - j)
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
