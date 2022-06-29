package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
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

  // UIManager.put("ComboBox.rendererUseListColors", Boolean.TRUE);
  val combo1 = JComboBox(model)
  combo1.renderer = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      return super.getListCellRendererComponent(
        list, "TEST0: $value", index, isSelected, cellHasFocus
      )
    }
  }
  // val d1 = UIDefaults();
  // d1.put("ComboBox.rendererUseListColors", true);
  // combo1.putClientProperty("Nimbus.Overrides", d1);

  val combo2 = JComboBox(model)
  combo2.renderer = object : BasicComboBoxRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      return super.getListCellRendererComponent(
        list, "TEST1: $value", index, isSelected, cellHasFocus
      )
    }
  }
  // combo2.putClientProperty("Nimbus.Overrides", d1);

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
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())
  EventQueue.invokeLater { p1.rootPane.jMenuBar = mb }

  val tabs = JTabbedPane()
  tabs.add("JList", p0)
  tabs.add("JComboBox", p1)
  return tabs
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
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
