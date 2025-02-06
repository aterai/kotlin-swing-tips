package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val popup = makePopupMenu()
  val tree = JTree()
  tree.componentPopupMenu = popup
  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopupMenu(): JPopupMenu {
  val p = JPopupMenu()
  p.add("add").addActionListener {
    val tree = p.invoker as? JTree
    val model = tree?.model as? DefaultTreeModel
    val path = tree?.selectionPath
    if (path != null && model != null) {
      val parent = path.getLastPathComponent() as? DefaultMutableTreeNode
      val child = DefaultMutableTreeNode("New node")
      model.insertNodeInto(child, parent, parent?.childCount ?: 0)
      tree.scrollPathToVisible(TreePath(child.path))
    }
  }
  p.addSeparator()
  p.add("remove").addActionListener {
    val tree = p.invoker as? JTree
    val paths = tree?.selectionPaths
    if (paths != null) {
      for (path in paths) {
        removeNode(tree, path)
      }
    }
  }
  p.addSeparator()
  p.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  p.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  return p
}

private fun removeNode(tree: JTree, path: TreePath) {
  val node = path.getLastPathComponent() as? DefaultMutableTreeNode
  if (node?.isRoot == false) {
    val model = tree.model as? DefaultTreeModel
    model?.removeNodeFromParent(node)
  }
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
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.put("PopupMenuUI", "example.RoundedPopupMenuUI")
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
