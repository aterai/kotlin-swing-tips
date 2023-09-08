package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode

fun makeUI(): Component {
  val tree1 = JTree(makeTreeRoot())
  expandAll(tree1)

  val tree2 = JTree(makeTreeRoot())
  expandAll(tree2)
  tree2.addTreeSelectionListener { e -> tree2.scrollPathToVisible(e.newLeadSelectionPath) }

  val key = "Tree.scrollsHorizontallyAndVertically"
  val check = JCheckBoxMenuItem(key)
  check.addActionListener { UIManager.put(key, check.isSelected) }

  val menu = JMenu("View")
  menu.add(check)

  val mb = JMenuBar()
  mb.add(menu)
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(GridLayout(1, 2)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeTitledPanel("Default", JScrollPane(tree1)))
    it.add(makeTitledPanel("TreeSelectionListener", JScrollPane(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTreeRoot(): DefaultMutableTreeNode {
  val set4 = DefaultMutableTreeNode("Set 00000004")
  set4.add(DefaultMutableTreeNode("222222111111111111111122222"))
  set4.add(DefaultMutableTreeNode("00000000000"))
  set4.add(DefaultMutableTreeNode("1111111111"))
  set4.add(DefaultMutableTreeNode("22222222"))
  val set3 = DefaultMutableTreeNode("Set 00000003")
  set3.add(DefaultMutableTreeNode("5555555555"))
  set3.add(set4)
  set3.add(DefaultMutableTreeNode("66666666666"))
  set3.add(DefaultMutableTreeNode("7777777777"))
  val set2 = DefaultMutableTreeNode("Set 00000002")
  set2.add(set3)
  set2.add(DefaultMutableTreeNode("333333333"))
  set2.add(DefaultMutableTreeNode("4444444444444"))
  val set1 = DefaultMutableTreeNode("Set 00000001")
  set1.add(DefaultMutableTreeNode("3333333333333333333333333333"))
  set1.add(set2)
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(DefaultMutableTreeNode("222222222"))
  val root = DefaultMutableTreeNode("Root")
  root.add(DefaultMutableTreeNode("888"))
  root.add(DefaultMutableTreeNode("99"))
  root.add(set1)
  root.add(DefaultMutableTreeNode("2222"))
  root.add(DefaultMutableTreeNode("11111"))
  return root
}

fun expandAll(tree: JTree) {
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row++)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
