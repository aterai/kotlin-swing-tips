package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.BorderUIResource
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val table = object : JTable(DefaultTableModel(15, 3)) {
    override fun updateUI() {
      val reset = ColorUIResource(Color.RED)
      setSelectionForeground(reset)
      setSelectionBackground(reset)
      super.updateUI()
      val showGrid = UIManager.getLookAndFeelDefaults()["Table.showGrid"] as? Boolean
      setShowGrid(showGrid == null || showGrid)
    }
  }
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.autoCreateRowSorter = true

  val scroll = JScrollPane(table)
  // scroll.border = BorderUIResource(BorderFactory.createLineBorder(Color.BLUE, 5))

  val key = "Table.scrollPaneBorder"
  val check = object : JCheckBox(key, UIManager.getBorder(key) != null) {
    override fun updateUI() {
      super.updateUI()
      val b = UIManager.getLookAndFeelDefaults().getBorder(key) != null
      updateTableScrollPane(scroll, key, b)
      isSelected = b
    }
  }
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    updateTableScrollPane(scroll, key, b)
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { table.rootPane.jMenuBar = mb }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateTableScrollPane(scroll: Component, key: String, lnf: Boolean) {
  val border = if (lnf) {
    UIManager.getLookAndFeelDefaults().getBorder(key)
  } else {
    BorderUIResource(BorderFactory.createEmptyBorder())
  }
  UIManager.put(key, border)
  SwingUtilities.updateComponentTreeUI(scroll)
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
