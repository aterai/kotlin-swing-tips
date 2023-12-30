package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.FontUIResource
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true

  val r = table.getDefaultRenderer(Boolean::class.javaObjectType)
  (r as? JCheckBox)?.putClientProperty("JComponent.sizeVariant", "mini")

  val p1 = JPanel(GridLayout(2, 1))
  p1.add(JScrollPane(table))
  p1.add(JScrollPane(JTree()))

  val p2 = JPanel(GridLayout(1, 2))
  p2.border = BorderFactory.createEmptyBorder(0, 5, 2, 5)
  p2.add(JLabel("JLabel"))
  p2.add(JCheckBox("JCheckBox"))
  p2.add(JButton("JButton"))

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(createSizeVariantMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JSlider(), BorderLayout.NORTH)
    it.add(p1)
    it.add(p2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun createSizeVariantMenu(): JMenu {
  val menu = JMenu("Resizing a Component")
  val bg = ButtonGroup()
  listOf("regular", "mini", "small", "large").forEach {
    menu.add(createSizeVariantItem(it, bg))
  }
  return menu
}

private fun createSizeVariantItem(
  key: String,
  bg: ButtonGroup,
): JMenuItem {
  val item = JRadioButtonMenuItem(key, "regular" == key)
  item.addActionListener {
    setSizeVariant(bg.selection.actionCommand)
  }
  item.actionCommand = key
  bg.add(item)
  return item
}

private fun setSizeVariant(key: String) {
  Window.getWindows().forEach {
    setSizeVariantAllComponents(it, key)
    SwingUtilities.updateComponentTreeUI(it)
    it.pack()
  }
}

private fun setSizeVariantAllComponents(
  parent: Container,
  key: String,
) {
  if (parent is JComponent) {
    parent.font = FontUIResource(parent.font)
    parent.putClientProperty("JComponent.sizeVariant", key)
  }
  for (c in parent.components) {
    if (c is Container) {
      setSizeVariantAllComponents(c, key)
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
