package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.ComboPopup

private val BACKGROUND = Color(70, 80, 90)

private val MODEL = arrayOf(
  "red",
  "pink",
  "orange",
  "yellow",
  "green",
  "magenta",
  "cyan",
  "blue",
)

fun makeUI(): Component {
  UIManager.put("ComboBox.forceOpaque", false)
  val ins = UIManager.getInsets("ComboBox.padding")
  ins.right = 0
  UIManager.put("ComboBox.padding", ins)
  val p = JPanel(GridLayout(0, 1))
  p.add(makeTitledPanel("ComboPopup", makeComboBox2()))
  p.add(makeTitledPanel("DefaultListCellRenderer", makeComboBox1()))
  p.add(makeTitledPanel("UIDefaults", makeComboBox0()))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ComboRenderer : DefaultListCellRenderer() {
  override fun getListCellRendererComponent(
    list: JList<*>,
    value: Any,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    list.background = BACKGROUND
    list.foreground = Color.WHITE
    list.selectionBackground = Color.LIGHT_GRAY
    list.selectionForeground = Color.BLACK
    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
  }
}

private fun makeComboBox0(): JComboBox<String> {
  val comboBox = JComboBox(MODEL)
  comboBox.foreground = Color.WHITE
  comboBox.background = BACKGROUND
  val d1 = UIDefaults()
  d1["ComboBox:\"ComboBox.listRenderer\".background"] = BACKGROUND
  d1["ComboBox:\"ComboBox.listRenderer\".textForeground"] = Color.WHITE
  d1["ComboBox:\"ComboBox.listRenderer\"[Selected].background"] = Color.LIGHT_GRAY
  d1["ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground"] = Color.BLACK
  val renderer = comboBox.renderer
  if (renderer is JComponent) {
    putClientProperty(renderer as JComponent, d1)
  }
  return comboBox
}

private fun makeComboBox1(): JComboBox<String> {
  return object : JComboBox<String>(MODEL) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      setRenderer(ComboRenderer())
      background = BACKGROUND
    }
  }
}

private fun makeComboBox2(): JComboBox<String> {
  return object : JComboBox<String>(MODEL) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val r = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        val c = r.getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        )
        (c as? JComponent)?.isOpaque = true
        c
      }
      val popup = getAccessibleContext().getAccessibleChild(0)
      if (popup is ComboPopup) {
        val list = popup.list
        list.background = BACKGROUND
        list.foreground = Color.WHITE
        list.selectionBackground = Color.LIGHT_GRAY
        list.selectionForeground = Color.BLACK
      }
      background = BACKGROUND
    }
  }
}

private fun putClientProperty(c: JComponent, d: UIDefaults) {
  c.putClientProperty("Nimbus.Overrides", d)
  c.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
}

private fun makeTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
  return p
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
