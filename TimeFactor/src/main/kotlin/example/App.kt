package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val key = "timeFactor"

fun makeUI(): Component {
  val lv = UIManager.get("Tree.$key") as? Number ?: 500
  val numberModel = SpinnerNumberModel(lv.toLong(), 0L, 5000L, 500L)
  UIManager.put("Tree.$key", 5000L)

  val model = arrayOf("a", "aa", "b", "bbb", "bbc")
  val combo = JComboBox(model)
  combo.prototypeDisplayValue = "M".repeat(30)

  val p = JPanel().also {
    it.add(JSpinner(numberModel))
    it.add(combo)
  }

  val tabbedPane = JTabbedPane().also {
    it.addTab("ComboBox.$key", p)
    it.addTab("List.$key", JScrollPane(JList(model)))
    it.addTab("Table.$key(JFileChooser)", JFileChooser())
    it.addTab("Tree.$key", JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
    EventQueue.invokeLater {
      val mb = JMenuBar()
      mb.add(LookAndFeelUtil.createLookAndFeelMenu())
      it.rootPane.jMenuBar = mb
    }
  }

  val panel = object :  JPanel(BorderLayout()) {
    override fun updateUI() {
      val v = numberModel.number.toLong()
      UIManager.put("ComboBox.$key", v)
      UIManager.put("List.$key", v)
      UIManager.put("Table.$key", v)
      UIManager.put("Tree.$key", v)
      super.updateUI()
    }
  }
  panel.add(tabbedPane)
  return panel
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
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel)
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
