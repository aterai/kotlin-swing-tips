package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer

fun makeUI(): Component {
  val root = DefaultMutableTreeNode(PluginNode("Plugins"))
  val model1 = listOf("Disabled", "Enabled", "Debug mode")
  root.add(DefaultMutableTreeNode(PluginNode("Plugin 1", model1)))
  root.add(DefaultMutableTreeNode(PluginNode("Plugin 2", model1)))
  val leaf = DefaultMutableTreeNode(PluginNode("Plugin 3"))
  root.add(leaf)
  val model2 = listOf("Disabled", "Enabled")
  leaf.add(DefaultMutableTreeNode(PluginNode("Plugin 3A", model2)))
  leaf.add(DefaultMutableTreeNode(PluginNode("Plugin 3B", model2)))

  val tree = JTree(root)
  tree.rowHeight = 0
  tree.isEditable = true
  tree.cellRenderer = PluginCellRenderer(JComboBox())
  tree.cellEditor = PluginCellEditor(JComboBox())

  val textArea = JTextArea(5, 1)

  tree.model.addTreeModelListener(object : TreeModelListener {
    override fun treeNodesChanged(e: TreeModelEvent) {
      val node = e.children?.takeIf { it.size == 1 }?.firstOrNull()
      (node as? DefaultMutableTreeNode)
        ?.let { it.userObject as? PluginNode }
        ?.let { "%s %s%n".format(it, it.plugins[it.selectedIndex]) }
        ?.also { textArea.append(it) }
    }

    override fun treeNodesInserted(e: TreeModelEvent) {
      /* not needed */
    }

    override fun treeNodesRemoved(e: TreeModelEvent) {
      /* not needed */
    }

    override fun treeStructureChanged(e: TreeModelEvent) {
      /* not needed */
    }
  })

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.add(JScrollPane(textArea), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private data class PluginNode(
  private val name: String,
  val plugins: List<String> = emptyList(),
  val selectedIndex: Int = 0
) {
  override fun toString() = name
}

private class PluginPanel(val comboBox: JComboBox<String>) : JPanel() {
  val pluginName = JLabel()

  init {
    comboBox.prototypeDisplayValue = "Debug mode x"
    isOpaque = false
    add(pluginName)
    add(comboBox)
  }

  fun extractNode(value: Any?): PluginNode? {
    val model = comboBox.model as? DefaultComboBoxModel<String>
    return ((value as? DefaultMutableTreeNode)?.userObject as? PluginNode)?.also {
      pluginName.text = it.toString()
      model?.removeAllElements()
      if (it.plugins.isEmpty()) {
        remove(comboBox)
      } else {
        add(comboBox)
        for (s in it.plugins) {
          model?.addElement(s)
        }
        comboBox.setSelectedIndex(it.selectedIndex)
      }
    }
  }
}

private class PluginCellRenderer(comboBox: JComboBox<String>) : TreeCellRenderer {
  private val panel = PluginPanel(comboBox)
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    panel.extractNode(value)
    return panel
  }
}

private class PluginCellEditor(comboBox: JComboBox<String>) : DefaultCellEditor(comboBox) {
  private val panel = PluginPanel(comboBox)

  @Transient
  private var node: PluginNode? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    isSelected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    node = panel.extractNode(value)
    return panel
  }

  override fun getCellEditorValue(): Any {
    val o = super.getCellEditorValue()
    return node
      ?.let {
        val idx = (panel.comboBox.model as? DefaultComboBoxModel<String>)?.getIndexOf(o) ?: -1
        val pn = PluginNode(panel.pluginName.text, it.plugins, idx)
        pn
      }
      ?: o
  }

  override fun isCellEditable(e: EventObject): Boolean {
    if (e is MouseEvent) {
      showComboPopup(e.component, e.point)
    }
    return delegate.isCellEditable(e)
  }

  private fun showComboPopup(cmp: Component, p: Point) {
    EventQueue.invokeLater {
      val pt = SwingUtilities.convertPoint(cmp, p, panel)
      val o = SwingUtilities.getDeepestComponentAt(panel, pt.x, pt.y)
      if (o is JComboBox<*>) {
        panel.comboBox.showPopup()
      } else if (o != null) { // maybe ArrowButton in JComboBox
        val c = SwingUtilities.getAncestorOfClass(JComboBox::class.java, o)
        (c as? JComboBox<*>)?.also {
          panel.comboBox.showPopup()
        }
      }
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
