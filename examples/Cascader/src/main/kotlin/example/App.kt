package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

fun createUI(): Component {
  val log = JTextArea()
  log.isEditable = false

  val combo1 = CascaderComboBox(createAddressModel(), false)
  combo1.addItemListener { appendLog(log, it) }

  val combo2 = CascaderComboBox(createCategoryModel(), true)
  combo2.addItemListener { appendLog(log, it) }

  val box = Box.createVerticalBox()
  box.add(createTitledPanel("Expand on click", combo1))
  box.add(Box.createVerticalStrut(5))
  box.add(createTitledPanel("Expand on hover", combo2))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createAddressModel() = createModel(
  "Japan/Tokyo/Chiyoda",
  "Japan/Tokyo/Chuo",
  "Japan/Tokyo/Minato",
  "Japan/Tokyo/Shinjuku",
  "Japan/Tokyo/Shibuya",
  "Japan/Osaka/Kita",
  "Japan/Osaka/Chuo",
  "Japan/Osaka/Naniwa",
  "Japan/Kyoto/Kamigyo",
  "Japan/Kyoto/Nakagyo",
  "Japan/Kyoto/Shimogyo",
  "Japan/Hokkaido/Sapporo",
  "Japan/Hokkaido/Hakodate",
  "USA/California/Los Angeles",
  "USA/California/San Francisco",
  "USA/California/San Diego",
  "USA/New York/New York City",
  "USA/New York/Buffalo",
  "USA/Texas/Houston",
  "USA/Texas/Austin",
  "UK/England/London",
  "UK/England/Manchester",
  "UK/Scotland/Edinburgh",
  "UK/Scotland/Glasgow",
)

private fun createCategoryModel() = createModel(
  "Electronics/Computers/Laptops",
  "Electronics/Computers/Desktops",
  "Electronics/Computers/Tablets",
  "Electronics/Computers/Accessories/Keyboards",
  "Electronics/Computers/Accessories/Mice",
  "Electronics/Computers/Accessories/Monitors",
  "Electronics/Phones/Smartphones",
  "Electronics/Phones/Feature Phones",
  "Electronics/Audio/Headphones",
  "Electronics/Audio/Speakers",
  "Books/Fiction/Mystery",
  "Books/Fiction/Science Fiction",
  "Books/Fiction/Fantasy",
  "Books/Non-Fiction/History",
  "Books/Non-Fiction/Science",
  "Books/Comics",
  "Home/Kitchen/Cookware",
  "Home/Kitchen/Tableware",
  "Home/Furniture/Chairs",
  "Home/Furniture/Tables",
  "Home/Furniture/Shelves",
)

private fun appendLog(log: JTextArea, e: ItemEvent) {
  val path = e.item
  if (e.stateChange == ItemEvent.SELECTED && path is TreePath) {
    log.append(CascaderComboBox.toText(path) + "\n")
  }
}

// Build a tree from slash separated paths: "Japan/Tokyo/Chiyoda"
private fun createModel(vararg paths: String): TreeModel {
  val root = DefaultMutableTreeNode("root")
  for (path in paths) {
    var parent = root
    for (name in path.split("/")) {
      parent = getOrCreateChild(parent, name)
    }
  }
  return DefaultTreeModel(root)
}

private fun getOrCreateChild(
  parent: DefaultMutableTreeNode,
  name: String,
): DefaultMutableTreeNode = parent
  .children()
  .toList()
  .filterIsInstance<DefaultMutableTreeNode>()
  .firstOrNull { it.userObject == name }
  ?: DefaultMutableTreeNode(name).also { parent.add(it) }

private fun createTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

// A JComboBox whose dropdown list is replaced with cascading JList columns.
// The ComboBoxModel has no elements and holds only the selected TreePath,
// so the TreeModel children are read lazily when each column is opened.
private class CascaderComboBox(
  private val treeModel: TreeModel,
  private val expandOnHover: Boolean,
) : JComboBox<TreePath>(DefaultComboBoxModel()) {
  private val columnPanel = JPanel()
  private val columns = mutableListOf<JList<Any>>()
  private val mouseHandler = CascadeMouseHandler()
  private var activeColumn = 0

  private val popup: JPopupMenu?
    // BasicComboBoxUI returns its ComboPopup (a JPopupMenu) as child 0
    get() = getUI()?.getAccessibleChild(this, 0) as? JPopupMenu

  init {
    columnPanel.layout = BoxLayout(columnPanel, BoxLayout.X_AXIS)
    setRenderer(object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
      ): Component {
        val text = (value as? TreePath)?.let { toText(it) } ?: PLACEHOLDER
        return super
          .getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
      }
    })
    addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        installColumnPanel()
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        // not needed
      }

      override fun popupMenuCanceled(e: PopupMenuEvent) {
        // not needed
      }
    })
    installCascadeActions()
  }

  fun setSelectedPath(path: TreePath) {
    // JComboBox#setSelectedItem(...) rejects an item that is not
    // in the model of a non-editable JComboBox, so set it to the model
    model.selectedItem = path
  }

  // Called by BasicComboPopup#show() before the popup location is computed
  private fun installColumnPanel() {
    val p = popup
    // The popup is recreated by updateUI(), so check it every time
    if (p != null && !p.isAncestorOf(columnPanel)) {
      p.removeAll() // remove the JScrollPane of the default JList
      p.add(columnPanel)
    }
    columnPanel.removeAll()
    columns.clear()
    setActiveColumn(0)
    addColumn(treeModel.root)
    // Restore the columns from the selected path
    val nodes = (model.selectedItem as? TreePath)?.path ?: return
    var i = 1
    while (i < nodes.size && i - 1 < columns.size) {
      val list = columns[i - 1]
      val idx = treeModel.getIndexOfChild(nodes[i - 1], nodes[i])
      if (idx < 0) {
        break // the rest of the path no longer exists in the TreeModel
      }
      // The ListSelectionListener appends the next column
      list.selectedIndex = idx
      list.ensureIndexIsVisible(idx)
      setActiveColumn(i - 1)
      i++
    }
  }

  private fun addColumn(parent: Any) {
    val list = JList<Any>(ChildListModel(treeModel, parent))
    list.isFocusable = false // keep the focus on the JComboBox
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION
    list.visibleRowCount = maximumRowCount
    list.fixedCellHeight = height
    list.cellRenderer =
      CascadeCellRenderer(treeModel) { indexOfColumn(it) == activeColumn }
    list.addListSelectionListener { selectionChanged(it) }
    list.addMouseListener(mouseHandler)
    list.addMouseMotionListener(mouseHandler)
    val scroll = JScrollPane(list)
    val left = if (columns.isEmpty()) 0 else 1
    scroll.border = BorderFactory.createMatteBorder(0, left, 0, 0, Color.LIGHT_GRAY)
    scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    columns.add(list)
    columnPanel.add(scroll)
  }

  private fun indexOfColumn(c: Any?) = columns.indexOfFirst { it === c }

  private fun selectionChanged(e: ListSelectionEvent) {
    val idx = indexOfColumn(e.source)
    if (!e.valueIsAdjusting && idx >= 0) {
      updateColumns(idx)
    }
  }

  private fun updateColumns(idx: Int) {
    // Remove the columns to the right of the changed column
    for (i in columns.size - 1 downTo idx + 1) {
      columns.removeAt(i)
      columnPanel.remove(i)
    }
    val value = columns[idx].selectedValue
    if (value != null && !treeModel.isLeaf(value)) {
      addColumn(value)
    }
    columnPanel.revalidate()
    columnPanel.repaint()
    // Resize the popup to the new number of columns
    popup?.takeIf { it.isVisible }?.pack()
  }

  private fun commit(columnIndex: Int) {
    val nodes =
      listOf(treeModel.root) + (0..columnIndex).map { columns[it].selectedValue }
    setPopupVisible(false)
    setSelectedPath(TreePath(nodes.toTypedArray()))
  }

  // Replace the key actions of the JComboBox while the popup is visible
  private fun installCascadeActions() {
    val am = actionMap
    // Open the popup with the arrow keys as the default actions do
    val show: (ActionEvent) -> Unit = { setPopupVisible(true) }
    val none: (ActionEvent) -> Unit = { /* do nothing */ }
    val next = createAction({ moveSelection(1) }, show)
    val prev = createAction({ moveSelection(-1) }, show)
    am.put("selectNext", next)
    am.put("selectNext2", next)
    am.put("selectPrevious", prev)
    am.put("selectPrevious2", prev)
    am.put("enterPressed", createAction(::commitOrMoveRight, ::fireDefaultEnterAction))
    am.put(CASCADE_RIGHT, createAction(::moveRight, none))
    am.put(CASCADE_LEFT, createAction(::moveLeft, none))
    val im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), CASCADE_RIGHT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), CASCADE_RIGHT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), CASCADE_LEFT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), CASCADE_LEFT)
  }

  private fun createAction(
    cascade: () -> Unit,
    fallback: (ActionEvent) -> Unit,
  ) = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (isPopupVisible && columns.isNotEmpty()) {
        cascade()
      } else {
        fallback(e)
      }
    }
  }

  // Delegate to the LookAndFeel action (e.g. press the default button)
  private fun fireDefaultEnterAction(e: ActionEvent) {
    actionMap.parent?.get("enterPressed")?.actionPerformed(e)
  }

  private fun setActiveColumn(index: Int) {
    if (activeColumn != index) {
      activeColumn = index
      // Repaint the selection colors of the active and inactive columns
      columnPanel.repaint()
    }
  }

  private fun moveSelection(step: Int) {
    val list = columns[activeColumn]
    val size = list.model.size
    if (size > 0) {
      val selected = list.selectedIndex
      val idx = if (selected < 0) {
        if (step > 0) 0 else size - 1
      } else {
        (selected + step).coerceIn(0, size - 1)
      }
      list.selectedIndex = idx
      list.ensureIndexIsVisible(idx)
    }
  }

  private fun moveRight() {
    if (activeColumn + 1 < columns.size) {
      setActiveColumn(activeColumn + 1)
      val list = columns[activeColumn]
      if (list.selectedIndex < 0 && list.model.size > 0) {
        list.selectedIndex = 0
      }
    }
  }

  private fun moveLeft() {
    if (activeColumn > 0) {
      // Clearing the selection also removes the columns to the right
      columns[activeColumn].clearSelection()
      setActiveColumn(activeColumn - 1)
    }
  }

  private fun commitOrMoveRight() {
    val value = columns[activeColumn].selectedValue ?: return
    if (treeModel.isLeaf(value)) {
      commit(activeColumn)
    } else {
      moveRight()
    }
  }

  private inner class CascadeMouseHandler : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      setActiveColumn(indexOfColumn(e.component).coerceAtLeast(0))
    }

    override fun mouseReleased(e: MouseEvent) {
      val list = e.component as? JList<*> ?: return
      val idx = getIndexAt(list, e.point)
      // Ctrl+click deselects the cell, so commit only the selected leaf
      val selectedLeaf = idx >= 0 && idx == list.selectedIndex &&
        treeModel.isLeaf(list.model.getElementAt(idx))
      if (SwingUtilities.isLeftMouseButton(e) && selectedLeaf) {
        commit(indexOfColumn(list))
      }
    }

    override fun mouseMoved(e: MouseEvent) {
      val list = e.component as? JList<*> ?: return
      val idx = getIndexAt(list, e.point)
      if (expandOnHover && idx >= 0) {
        // Activate the hovered column even if its cell is already selected
        setActiveColumn(indexOfColumn(list))
        list.selectedIndex = idx
      }
    }

    private fun getIndexAt(
      list: JList<*>,
      pt: Point,
    ): Int {
      // JList#locationToIndex(...) returns the last index below the last cell
      val idx = list.locationToIndex(pt)
      val r = if (idx < 0) null else list.getCellBounds(idx, idx)
      return if (r?.contains(pt) == true) idx else -1
    }
  }

  companion object {
    private const val PLACEHOLDER = "Please select"
    private const val CASCADE_RIGHT = "cascadeRight"
    private const val CASCADE_LEFT = "cascadeLeft"

    // Skip the root node
    fun toText(path: TreePath) = path.path.drop(1).joinToString(" / ")
  }
}

// A ListModel that reads the children of a TreeModel node on demand
private class ChildListModel(
  private val model: TreeModel,
  private val parent: Any,
) : AbstractListModel<Any>() {
  override fun getSize() = model.getChildCount(parent)

  override fun getElementAt(index: Int): Any = model.getChild(parent, index)
}

private class CascadeCellRenderer(
  private val model: TreeModel,
  private val isActive: (JList<*>) -> Boolean,
) : ListCellRenderer<Any> {
  private val renderer = DefaultListCellRenderer()
  private val panel = JPanel(BorderLayout())
  private val arrow = JLabel()
  private val arrowIcon = ArrowIcon()
  private val emptyIcon = EmptyIcon(arrowIcon)

  init {
    // Paint the background with the JPanel only, since Nimbus ignores
    // the ColorUIResource background of a JLabel or JPanel
    renderer.isOpaque = false
    arrow.border = BorderFactory.createEmptyBorder(0, 8, 0, 4)
    panel.add(renderer)
    panel.add(arrow, BorderLayout.EAST)
  }

  override fun getListCellRendererComponent(
    list: JList<out Any>,
    value: Any?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, false)
    var bgc = c.background
    if (isSelected && !isActive(list)) {
      // Show the selected path of the parent columns in a lighter color
      bgc = blend(list.selectionBackground, list.background)
      c.foreground = list.foreground
    }
    // A plain Color (not a ColorUIResource) is required for Nimbus
    panel.background = Color(bgc.rgb)
    // Keep the width of the arrow on the leaf rows to align the text
    arrow.icon = if (model.isLeaf(value)) emptyIcon else arrowIcon
    arrow.foreground = c.foreground
    return panel
  }

  private fun blend(
    c1: Color,
    c2: Color,
  ) = Color(
    (c1.red + c2.red * 3) / 4,
    (c1.green + c2.green * 3) / 4,
    (c1.blue + c2.blue * 3) / 4,
  )
}

private class ArrowIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2
      .setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
    g2.translate(x, y)
    g2.paint = c.foreground
    // A thin stroke similar to the arrow button of the JComboBox
    g2.stroke = BasicStroke(1f)
    val p = Path2D.Double()
    p.moveTo(2.0, 1.0)
    p.lineTo(6.0, 5.0)
    p.lineTo(2.0, 9.0)
    g2.draw(p)
    g2.dispose()
  }

  override fun getIconWidth() = 8

  override fun getIconHeight() = 10
}

private class EmptyIcon(
  private val icon: Icon,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // Empty icon
  }

  override fun getIconWidth() = icon.iconWidth

  override fun getIconHeight() = icon.iconHeight
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
