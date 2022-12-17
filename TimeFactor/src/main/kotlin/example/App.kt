package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val TREE_KEY = "Tree.timeFactor"

fun makeUI(): Component {
  val lv = UIManager.get(TREE_KEY) as? Number ?: 500
  val numberModel = SpinnerNumberModel(lv.toLong(), 0L, 5000L, 500L)
  UIManager.put(TREE_KEY, 5000L)

  val model = arrayOf("a", "aa", "b", "bbb", "bbc")
  val combo = JComboBox(model)
  combo.prototypeDisplayValue = "M".repeat(30)

  val p = JPanel().also {
    it.add(JSpinner(numberModel))
    it.add(combo)
  }

  val tabbedPane = JTabbedPane().also {
    it.addTab("ComboBox.timeFactor", p)
    it.addTab("List.timeFactor", JScrollPane(JList(model)))
    it.addTab("Table.timeFactor(JFileChooser)", JFileChooser())
    it.addTab(TREE_KEY, JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
    EventQueue.invokeLater {
      val mb = JMenuBar()
      mb.add(LookAndFeelUtils.createLookAndFeelMenu())
      it.rootPane.jMenuBar = mb
    }
  }

  val panel = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      val v = numberModel.number.toLong()
      UIManager.put("ComboBox.timeFactor", v)
      UIManager.put("List.timeFactor", v)
      UIManager.put("Table.timeFactor", v)
      UIManager.put(TREE_KEY, v)
      super.updateUI()
    }
  }
  panel.add(tabbedPane)
  return panel
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
