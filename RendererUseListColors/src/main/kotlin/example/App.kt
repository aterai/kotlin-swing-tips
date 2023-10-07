package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxRenderer

fun makeUI(): Component {
  val model = arrayOf("Red", "Green", "Blue")
  val list = JList(model)
  val d = UIDefaults()
  d["List.rendererUseListColors"] = true
  val key = "Nimbus.Overrides"
  list.putClientProperty(key, d)

  val p0 = JPanel(GridLayout(1, 2))
  p0.add(JScrollPane(JList(model)))
  p0.add(JScrollPane(list))
  val combo0 = object : JComboBox<String>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val def = UIDefaults()
      def["ComboBox.rendererUseListColors"] = true
      putClientProperty(key, def)
    }
  }

  // UIManager.put("ComboBox.rendererUseListColors", Boolean.TRUE)
  val combo1 = JComboBox(model)
  combo1.renderer = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean,
    ): Component {
      return super.getListCellRendererComponent(
        list,
        "TEST0: $value",
        index,
        isSelected,
        cellHasFocus,
      )
    }
  }
  // val d1 = UIDefaults()
  // d1.put("ComboBox.rendererUseListColors", true)
  // combo1.putClientProperty("Nimbus.Overrides", d1)

  val combo2 = JComboBox(model)
  combo2.renderer = object : BasicComboBoxRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean,
    ): Component {
      return super.getListCellRendererComponent(
        list,
        "TEST1: $value",
        index,
        isSelected,
        cellHasFocus,
      )
    }
  }
  // combo2.putClientProperty("Nimbus.Overrides", d1)

  val combo3 = object : JComboBox<String>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val def = UIDefaults()
      def["ComboBox.rendererUseListColors"] = false
      putClientProperty(key, def)
      val r = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        val c = r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (c is JLabel) {
          c.text = "TEST2: $value"
        }
        c
      }
    }
  }
  val grid = JPanel(GridLayout(2, 2))
  grid.add(makeTitledPanel("ComboBox.rendererUseListColors: true", combo0))
  grid.add(makeTitledPanel("DefaultListCellRenderer", combo1))
  grid.add(makeTitledPanel("BasicComboBoxRenderer", combo2))
  grid.add(makeTitledPanel("SynthComboBoxRenderer + ListCellRenderer", combo3))

  val p1 = JPanel(BorderLayout())
  p1.add(grid, BorderLayout.NORTH)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { p1.rootPane.jMenuBar = mb }

  val tabs = JTabbedPane()
  tabs.add("JList", p0)
  tabs.add("JComboBox", p1)
  return tabs
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
