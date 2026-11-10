package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.text.Position
import javax.swing.tree.TreeNode

fun createUI(): Component {
  val title0 = "Box + Multiple JList + JSeparator"
  val list0 = makeListBox()
  val title1 = "JSeparator + ListModel"
  val list1 = GroupList(makeSeparatorModel())
  val title2 = "CellRenderer + MatteBorder"
  val list2 = GroupBorderList(makeTreeNodeModel())
  return JPanel(GridLayout(1, 0)).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(makeTitledPanel(title0, list0))
    it.add(makeTitledPanel(title1, list1))
    it.add(makeTitledPanel(title2, list2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val scroll = JScrollPane(c)
  scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(scroll)
  return p
}

private fun makeListBox(): Container {
  val box = Box.createVerticalBox()
  getGroups().forEach { node ->
    box.add(makeList(getChildren(node)))
    box.add(JSeparator())
  }
  val p = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      // A color set only once would be reset to Panel.background by a LookAndFeel change
      background = UIManager.getColor("List.background")
    }
  }
  p.add(box, BorderLayout.NORTH)
  return p
}

private fun makeList(items: List<Any>): JList<Any> {
  val list = object : JList<Any>(items.toTypedArray()) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val r = cellRenderer
      setCellRenderer { l, v, index, isSelected, cellHasFocus ->
        val selected = isSelected && l.isFocusOwner
        r.getListCellRendererComponent(l, v, index, selected, cellHasFocus)
      }
    }

    override fun processFocusEvent(e: FocusEvent) {
      super.processFocusEvent(e)
      // ListUI repaints only the lead cell, but every selected cell depends on the focus
      repaint()
    }
  }
  val height = list.preferredSize.height
  list.maximumSize = Dimension(Short.MAX_VALUE.toInt(), height)
  return list
}

private fun makeSeparatorModel(): ListModel<Any> {
  val model = DefaultListModel<Any>()
  getGroups().forEach { node ->
    if (!model.isEmpty) {
      model.addElement(JSeparator())
    }
    getChildren(node).forEach { model.addElement(it) }
  }
  return model
}

private fun makeTreeNodeModel(): ListModel<TreeNode> {
  val model = DefaultListModel<TreeNode>()
  getGroups().flatMap { getChildren(it) }.forEach { model.addElement(it) }
  return model
}

private fun getGroups(): List<TreeNode> {
  val root = JTree().model.root as? TreeNode ?: return emptyList()
  return getChildren(root).filterNot { it.isLeaf }
}

private fun getChildren(parent: TreeNode): List<TreeNode> = parent.children().toList()

private class GroupList<E>(
  model: ListModel<E>,
) : JList<E>(model) {
  init {
    skipSeparator(actionMap, "selectPreviousRow")
    skipSeparator(actionMap, "selectNextRow")
  }

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    // The JSeparator in the ListModel is out of the component tree and would keep the old UI
    val separator = JSeparator()
    val r = cellRenderer
    setCellRenderer { l, v, index, isSelected, cellHasFocus ->
      if (v is JSeparator) {
        separator
      } else {
        r.getListCellRendererComponent(l, v, index, isSelected, cellHasFocus)
      }
    }
  }

  override fun getNextMatch(
    prefix: String,
    startIndex: Int,
    bias: Position.Bias,
  ): Int {
    // Keep the type-ahead search from matching JSeparator#toString()
    val index = super.getNextMatch(prefix, startIndex, bias)
    return if (isSeparator(this, index)) -1 else index
  }
}

private fun skipSeparator(
  am: ActionMap,
  key: String,
) {
  val action = am.get(key)
  am.put(
    key,
    object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        action.actionPerformed(e)
        val l = e.source as? JList<*> ?: return
        if (isSeparator(l, l.selectedIndex)) {
          action.actionPerformed(e)
        }
      }
    },
  )
}

private fun isSeparator(
  list: JList<*>,
  index: Int,
) = index >= 0 && list.model.getElementAt(index) is JSeparator

private class GroupBorderList<E : TreeNode>(
  model: ListModel<E>,
) : JList<E>(model) {
  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    val r = cellRenderer
    setCellRenderer { l, v, index, isSelected, cellHasFocus ->
      val c = r.getListCellRendererComponent(l, v, index, isSelected, cellHasFocus)
      if (c is JComponent) {
        // The default renderer resets its focus Border on every call, so it never nests
        val outside = if (isGroupEnd(l, v, index)) GROUP_END else GROUP_ITEM
        c.border = BorderFactory.createCompoundBorder(outside, c.border)
      }
      c
    }
  }

  companion object {
    private val GROUP_END = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY)
    private val GROUP_ITEM = BorderFactory.createEmptyBorder(0, 0, 1, 0)

    private fun isGroupEnd(
      l: JList<out TreeNode>,
      v: TreeNode,
      index: Int,
    ): Boolean {
      val m = l.model
      val next = index + 1
      return next < m.size && m.getElementAt(next).parent != v.parent
    }
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
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
