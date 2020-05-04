package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val log = JTextArea()
  val key = "FileView.fullRowSelection"
  println(UIManager.getLookAndFeelDefaults().getBoolean(key))

  val check = object : JCheckBox(key) {
    override fun updateUI() {
      super.updateUI()
      isSelected = UIManager.getLookAndFeelDefaults().getBoolean(key)
    }
  }

  val button = JButton("open")
  button.addActionListener { e: ActionEvent ->
    val flg = check.isSelected
    UIManager.put("FileView.fullRowSelection", flg)
    val chooser = JFileChooser()
    chooser.actionMap["viewTypeDetails"]?.actionPerformed(
      ActionEvent(e.source, e.id, "viewTypeDetails"))

    descendants(chooser)
      .filterIsInstance(JTable::class.java)
      .firstOrNull()?.putClientProperty("Table.isFileList", !flg)

    val retValue = chooser.showOpenDialog(button.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val p = JPanel()
  p.add(check)
  p.add(button)

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    SwingUtilities.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

fun descendants(parent: Container): List<Component> {
  return parent.components.toList()
    .filterIsInstance(Container::class.java)
    .map { descendants(it) }
    .fold(listOf<Component>(parent)) { a, b -> a + b }
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
    lafItem.mnemonic = lafName.codePointAt(0)
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
