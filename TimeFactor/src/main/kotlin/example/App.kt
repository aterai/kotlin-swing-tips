package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  // private val spinner: JSpinner? = JSpinner()
  private val snmodel: SpinnerNumberModel?

  init {
    val lv = UIManager.get("Tree.timeFactor") as? Number ?: 500
    snmodel = SpinnerNumberModel(lv.toLong(), 0L, 5000L, 500L)
    UIManager.put("List.timeFactor", 5000L)

    val model = arrayOf("a", "aa", "b", "bbb", "bbc")
    val combo = JComboBox<String>(model)
    combo.setPrototypeDisplayValue("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMM")

    val p = JPanel().also {
      it.add(JSpinner(snmodel))
      it.add(combo)
    }

    val tabbedPane = JTabbedPane().also {
      it.addTab("ComboBox.timeFactor", p)
      it.addTab("List.timeFactor", JScrollPane(JList<String>(model)))
      it.addTab("Table.timeFactor(JFileChooser)", JFileChooser())
      it.addTab("Tree.timeFactor", JScrollPane(JTree()))
    }

    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }

  override fun updateUI() {
    // Unnecessary safe call on a non-null receiver of type JSpinner
    // private val spinner = JSpinner()
    // val lv = spinner
    //     ?.let { it.getModel().getValue() }
    //     ?: 1000L

    // NullPointerException
    // private val spinner = JSpinner()
    // val lv = spinner.getModel().getValue()

    // Condition 'spinner != null' is always 'true'
    // private val spinner = JSpinner()
    // val lv = if (spinner != null) spinner.getModel().getValue() else 1000L

    // private val spinner = JSpinner()
    // val lv = Optional.ofNullable(spinner)
    //     .map { it.getModel().getValue() }
    //     .orElse(1000L)

    // private val spinner: JSpinner? = JSpinner()
    // val lv = spinner?.getModel()?.getValue() ?: 1000L

    val lv = snmodel?.getNumber()?.toLong() ?: 1000L

    UIManager.put("ComboBox.timeFactor", lv)
    UIManager.put("List.timeFactor", lv)
    UIManager.put("Table.timeFactor", lv)
    UIManager.put("Tree.timeFactor", lv)
    super.updateUI()
  }
}

internal object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()

  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafRadioGroup.getSelection()
      runCatching {
        setLookAndFeel(m.getActionCommand())
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
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }
  }

  private fun updateLookAndFeel() {
    for (window in Frame.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())

    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      setJMenuBar(mb)
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
