package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.EventObject
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

fun makeUI(): Component {
  val list1 = Box.createVerticalBox()

  val model = DefaultListModel<CheckBoxNode>()
  val list2 = CheckBoxList(model)

  val list3 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      isEditable = true
      isRootVisible = false
      setShowsRootHandles(false)
      setCellRenderer(CheckBoxNodeRenderer())
      setCellEditor(CheckBoxNodeEditor())
    }
  }

  val p = JPanel(GridLayout(1, 3)).also {
    it.add(makeTitledPanel("Box", JScrollPane(list1)))
    it.add(makeTitledPanel("JList", JScrollPane(list2)))
    it.add(makeTitledPanel("JTree", JScrollPane(list3)))
  }

  val root = DefaultMutableTreeNode("JTree")
  listOf(
    "aaa", "bbb bb bb", "ccc", "dd dd dd", "eee eee",
    "ff ff ff fff", "ggg ggg", "hhh hh", "ii ii", "jjj jjj jj jj",
  ).forEach {
    val isSelected = it.length % 2 == 0
    val c = JCheckBox(it, isSelected)
    c.alignmentX = Component.LEFT_ALIGNMENT
    list1.add(c)
    model.addElement(CheckBoxNode(it, isSelected))
    root.add(DefaultMutableTreeNode(CheckBoxNode(it, isSelected)))
  }
  list3.model = DefaultTreeModel(root)

  return JPanel(BorderLayout()).also {
    it.add(JLabel("JCheckBox in ", SwingConstants.CENTER), BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private data class CheckBoxNode(val text: String, val selected: Boolean) {
  override fun toString() = text
}

private class CheckBoxList(model: ListModel<CheckBoxNode>) : JList<CheckBoxNode>(model) {
  private var renderer: CheckBoxCellRenderer? = null
  private var handler: MouseListener? = null

  override fun updateUI() {
    foreground = null
    background = null
    selectionForeground = null
    selectionBackground = null
    removeMouseListener(handler)
    removeMouseListener(renderer)
    removeMouseMotionListener(renderer)
    super.updateUI()
    handler = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val index = locationToIndex(e.point)
        if (e.button == MouseEvent.BUTTON1 && index >= 0) {
          (model as? DefaultListModel<CheckBoxNode>)?.also {
            val node = it.get(index)
            it.set(index, CheckBoxNode(node.text, !node.selected))
            repaint(getCellBounds(index, index))
          }
        }
      }
    }
    addMouseListener(handler)
    renderer = CheckBoxCellRenderer()
    cellRenderer = renderer
    addMouseListener(renderer)
    addMouseMotionListener(renderer)
    putClientProperty("List.isFileList", true)
  }

  // @see SwingUtilities2.pointOutsidePrefSize(...)
  private fun pointOutsidePrefSize(p: Point): Boolean {
    val i = locationToIndex(p)
    val cbn = model.getElementAt(i)
    val c = cellRenderer.getListCellRendererComponent(this, cbn, i, false, false)
    val rect = getCellBounds(i, i)
    rect.width = c.preferredSize.width
    return i < 0 || !rect.contains(p)
  }

  override fun processMouseEvent(e: MouseEvent) {
    if (!pointOutsidePrefSize(e.point)) {
      super.processMouseEvent(e)
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    if (pointOutsidePrefSize(e.point)) {
      val ev = MouseEvent(
        e.component, MouseEvent.MOUSE_EXITED, e.getWhen(), e.modifiersEx,
        e.x, e.y, e.xOnScreen, e.yOnScreen, e.clickCount,
        e.isPopupTrigger, MouseEvent.NOBUTTON,
      )
      super.processMouseEvent(ev)
    } else {
      super.processMouseMotionEvent(e)
    }
  }
}

private class CheckBoxCellRenderer : MouseAdapter(), ListCellRenderer<CheckBoxNode> {
  private val checkBox = JCheckBox()
  private var rollOverRowIndex = -1

  override fun getListCellRendererComponent(
    list: JList<out CheckBoxNode>,
    value: CheckBoxNode,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    checkBox.isOpaque = true
    if (isSelected) {
      checkBox.background = list.selectionBackground
      checkBox.foreground = list.selectionForeground
    } else {
      checkBox.background = list.background
      checkBox.foreground = list.foreground
    }
    checkBox.isSelected = value.selected
    checkBox.model.isRollover = index == rollOverRowIndex
    checkBox.text = value.text
    return checkBox
  }

  override fun mouseExited(e: MouseEvent) {
    val l = e.component
    if (l is JList<*> && rollOverRowIndex >= 0) {
      l.repaint(l.getCellBounds(rollOverRowIndex, rollOverRowIndex))
      rollOverRowIndex = -1
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    (e.component as? JList<*>)?.also {
      val index = it.locationToIndex(e.point)
      if (index != rollOverRowIndex) {
        rollOverRowIndex = index
        it.repaint()
      }
    }
  }
}

private class CheckBoxNodeRenderer : TreeCellRenderer {
  private val checkBox = JCheckBox()
  private val tcr = DefaultTreeCellRenderer()

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    if (leaf && value is DefaultMutableTreeNode) {
      checkBox.isOpaque = false
      val userObject = value.userObject
      if (userObject is CheckBoxNode) {
        checkBox.text = userObject.text
        checkBox.isSelected = userObject.selected
      }
      return checkBox
    }
    return tcr.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
  }
}

// delegation pattern
private class CheckBoxNodeEditor : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = object : JCheckBox() {
    private var handler: ActionListener? = null

    override fun updateUI() {
      removeActionListener(handler)
      super.updateUI()
      isOpaque = false
      isFocusable = false
      handler = ActionListener { stopCellEditing() }
      addActionListener(handler)
    }
  }

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
  ): Component {
    if (leaf && value is DefaultMutableTreeNode) {
      val userObject = value.userObject
      if (userObject is CheckBoxNode) {
        checkBox.isSelected = userObject.selected
      } else {
        checkBox.isSelected = false
      }
      checkBox.text = value.toString()
    }
    return checkBox
  }

  override fun getCellEditorValue() = CheckBoxNode(checkBox.text, checkBox.isSelected)

  override fun isCellEditable(e: EventObject?) = e is MouseEvent
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
