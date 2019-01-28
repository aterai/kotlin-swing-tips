package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val list1 = Box.createVerticalBox()

    val model = DefaultListModel<CheckBoxNode>()
    val list2 = CheckBoxList<CheckBoxNode>(model)

    val list3 = object : JTree() {
      override fun updateUI() {
        setCellRenderer(null)
        setCellEditor(null)
        super.updateUI()
        setEditable(true)
        setRootVisible(false)
        setShowsRootHandles(false)
        setCellRenderer(CheckBoxNodeRenderer())
        setCellEditor(CheckBoxNodeEditor())
      }
    }

    val p = JPanel(GridLayout(1, 3))
    p.add(makeTitledPanel("Box", JScrollPane(list1)))
    p.add(makeTitledPanel("JList", JScrollPane(list2)))
    p.add(makeTitledPanel("JTree", JScrollPane(list3)))

    val root = DefaultMutableTreeNode("JTree")
    listOf("aaaa", "bbbbbbb", "ccc", "dddddd", "eeeeeee", "fffffffff",
          "gggggg", "hhhhh", "iiii", "jjjjjjjjjj").forEach {
      val isSelected = it.length % 2 == 0
      val c = JCheckBox(it, isSelected)
      c.setAlignmentX(Component.LEFT_ALIGNMENT)
      list1.add(c)
      model.addElement(CheckBoxNode(it, isSelected))
      root.add(DefaultMutableTreeNode(CheckBoxNode(it, isSelected)))
    }
    list3.setModel(DefaultTreeModel(root))

    add(JLabel("JCheckBox in ", SwingConstants.CENTER), BorderLayout.NORTH)
    add(p)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }
}

open class CheckBoxNode(val text: String, val selected: Boolean) {
  override fun toString(): String {
    return text
  }
}

class CheckBoxList<E : CheckBoxNode>(model: ListModel<E>) : JList<E>(model) {
  private var renderer: CheckBoxCellRenderer<E>? = null

  override fun updateUI() {
    setForeground(null)
    setBackground(null)
    setSelectionForeground(null)
    setSelectionBackground(null)
    removeMouseListener(renderer)
    removeMouseMotionListener(renderer)
    super.updateUI()
    renderer = CheckBoxCellRenderer()
    setCellRenderer(renderer)
    addMouseListener(renderer)
    addMouseMotionListener(renderer)
    putClientProperty("List.isFileList", java.lang.Boolean.TRUE)
  }

  // @see SwingUtilities2.pointOutsidePrefSize(...)
  private fun pointOutsidePrefSize(p: Point): Boolean {
    val i = locationToIndex(p)
    val cbn = getModel().getElementAt(i)
    val c = getCellRenderer().getListCellRendererComponent(this, cbn, i, false, false)
    val rect = getCellBounds(i, i)
    rect.width = c.getPreferredSize().width
    return i < 0 || !rect.contains(p)
  }

  protected override fun processMouseEvent(e: MouseEvent) {
    if (!pointOutsidePrefSize(e.getPoint())) {
      super.processMouseEvent(e)
    }
  }

  protected override fun processMouseMotionEvent(e: MouseEvent) {
    if (pointOutsidePrefSize(e.getPoint())) {
      val ev = MouseEvent(
          e.getComponent(), MouseEvent.MOUSE_EXITED, e.getWhen(), e.getModifiersEx(),
          e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(),
          e.isPopupTrigger(), MouseEvent.NOBUTTON)
      super.processMouseEvent(ev)
    } else {
      super.processMouseMotionEvent(e)
    }
  }
}

class CheckBoxCellRenderer<E : CheckBoxNode> : MouseAdapter(), ListCellRenderer<E> {
  private val checkBox = JCheckBox()
  private var rollOverRowIndex = -1
  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    checkBox.setOpaque(true)
    if (isSelected) {
      checkBox.setBackground(list.getSelectionBackground())
      checkBox.setForeground(list.getSelectionForeground())
    } else {
      checkBox.setBackground(list.getBackground())
      checkBox.setForeground(list.getForeground())
    }
    checkBox.setSelected(value.selected)
    checkBox.getModel().setRollover(index == rollOverRowIndex)
    checkBox.setText(value.text)
    return checkBox
  }

  override fun mouseExited(e: MouseEvent) {
    if (rollOverRowIndex >= 0) {
      val l = e.getComponent() as JList<*>
      l.repaint(l.getCellBounds(rollOverRowIndex, rollOverRowIndex))
      rollOverRowIndex = -1
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun mouseClicked(e: MouseEvent) {
    if (e.getButton() == MouseEvent.BUTTON1) {
      val l = e.getComponent() as JList<CheckBoxNode>
      val p = e.getPoint()
      val index = l.locationToIndex(p)
      if (index >= 0) {
        val m = l.getModel() as DefaultListModel<CheckBoxNode>
        val n = m.get(index)
        m.set(index, CheckBoxNode(n.text, !n.selected))
        l.repaint(l.getCellBounds(index, index))
      }
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    val l = e.getComponent() as JList<*>
    val index = l.locationToIndex(e.getPoint())
    if (index != rollOverRowIndex) {
      rollOverRowIndex = index
      l.repaint()
    }
  }
}

class CheckBoxNodeRenderer : TreeCellRenderer {
  private val checkBox = JCheckBox()
  private val renderer = DefaultTreeCellRenderer()
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    if (leaf && value is DefaultMutableTreeNode) {
      checkBox.setOpaque(false)
      val userObject = value.getUserObject()
      if (userObject is CheckBoxNode) {
        checkBox.setText(userObject.text)
        checkBox.setSelected(userObject.selected)
      }
      return checkBox
    }
    return renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
  }
}

// delegation pattern
class CheckBoxNodeEditor : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = object : JCheckBox() {
    @Transient
    protected var handler: ActionListener? = null

    override fun updateUI() {
      removeActionListener(handler)
      super.updateUI()
      setOpaque(false)
      setFocusable(false)
      handler = ActionListener { stopCellEditing() }
      addActionListener(handler)
    }
  }

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    if (leaf && value is DefaultMutableTreeNode) {
      val userObject = value.getUserObject()
      if (userObject is CheckBoxNode) {
        checkBox.setSelected(userObject.selected)
      } else {
        checkBox.setSelected(false)
      }
      checkBox.setText(value.toString())
    }
    return checkBox
  }

  override fun getCellEditorValue(): Any {
    return CheckBoxNode(checkBox.getText(), checkBox.isSelected())
  }

  override fun isCellEditable(e: EventObject?): Boolean {
    return e is MouseEvent
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
