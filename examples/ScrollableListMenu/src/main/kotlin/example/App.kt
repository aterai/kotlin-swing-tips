package example

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun createUI(): Component {
  val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
  val model = DefaultListModel<String>()
  fonts.map { it.fontName }.forEach { model.addElement(it) }
  val list = PopupList(model)
  // The unit increment of the vertical scroll bar is taken from
  // JList#getScrollableUnitIncrement(...), i.e. the row height
  val scroll = JScrollPane(list)
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()
  val subMenu = JMenu("Font list")
  subMenu.add(scroll)
  val menu = JMenu("Menu")
  menu.add(subMenu)
  menu.addSeparator()
  menu.add("Item 1")
  menu.add("Item 2")
  val menuBar = JMenuBar()
  menuBar.add(menu)
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  val textArea = JTextArea("The quick brown fox jumps over the lazy dog.")
  textArea.lineWrap = true
  list.addListSelectionListener { e ->
    val name = list.selectedValue
    if (!e.valueIsAdjusting && name != null) {
      val font = textArea.font
      textArea.font = Font(name, font.style, font.size)
    }
  }
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class PopupList<E>(
  model: ListModel<E>,
) : JList<E>(model) {
  private var listener: PopupListMouseListener? = null
  private var renderer: RadioButtonMenuItemCellRenderer<E>? = null
  private var typeAhead = false

  val isDoNotCloseOnMouseClick: Boolean
    get() = renderer?.isDoNotCloseOnMouseClick ?: false

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    super.updateUI()
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    val l = PopupListMouseListener()
    listener = l
    addMouseListener(l)
    addMouseMotionListener(l)
    // Recreate the renderer so that its JRadioButtonMenuItem uses the new LookAndFeel
    val r = RadioButtonMenuItemCellRenderer<E>(l)
    renderer = r
    cellRenderer = r
  }

  override fun processKeyEvent(e: KeyEvent) {
    // BasicListUI selects the item found by the type-ahead search
    // and calls ensureIndexIsVisible(...) in KeyListener#keyTyped(...)
    typeAhead = e.id == KeyEvent.KEY_TYPED
    if (typeAhead) {
      // Clear the rollover so that the focused (lead) item is highlighted
      listener?.clearRolloverIndex(this)
    }
    try {
      super.processKeyEvent(e)
    } finally {
      typeAhead = false
    }
  }

  override fun ensureIndexIsVisible(index: Int) {
    val cell = getCellBounds(index, index)
    if (typeAhead && cell != null) {
      // Scroll so that the item found by the type-ahead search is centered,
      // JViewport#scrollRectToVisible(...) clamps the position to the view bounds
      val r = visibleRect
      r.y = cell.y - (r.height - cell.height) / 2
      scrollRectToVisible(r)
    } else {
      super.ensureIndexIsVisible(index)
    }
  }
}

private class RadioButtonMenuItemCellRenderer<E>(
  private val listener: PopupListMouseListener,
) : ListCellRenderer<E> {
  private val item = JRadioButtonMenuItem()

  init {
    // Java 9+: same client property as JRadioButtonMenuItem (JDK-8165234),
    // but a click on the JList cell does not go through BasicMenuItemUI,
    // so PopupListMouseListener refers to this value by itself
    item.putClientProperty(DO_NOT_CLOSE, true)
  }

  // Same lookup order as BasicMenuItemUI: client property, then UIManager
  val isDoNotCloseOnMouseClick: Boolean
    get() = item.getClientProperty(DO_NOT_CLOSE) as? Boolean
      ?: UIManager.getBoolean(DO_NOT_CLOSE)

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    item.text = value?.toString() ?: ""
    // The selected list item is shown as a checked radio button
    item.isSelected = isSelected
    // The rollover list item, or the focused (lead) list item if there is no rollover,
    // is painted with the menu item highlight of the LookAndFeel
    val focused = cellHasFocus && !listener.hasRollover()
    item.isArmed = listener.isRolloverIndex(index) || focused
    return item
  }

  companion object {
    const val DO_NOT_CLOSE = "RadioButtonMenuItem.doNotCloseOnMouseClick"
  }
}

private class PopupListMouseListener : MouseAdapter() {
  private var rolloverIndex = -1

  fun isRolloverIndex(i: Int) = rolloverIndex == i

  fun hasRollover() = rolloverIndex >= 0

  fun clearRolloverIndex(list: JList<*>) {
    setRolloverIndex(list, -1)
  }

  private fun updateRolloverIndex(e: MouseEvent) {
    val list = e.component as? JList<*> ?: return
    val pt = e.point
    val i = list.locationToIndex(pt)
    // locationToIndex(...) returns the nearest cell index,
    // so ignore points outside the cell bounds (e.g. below the last item)
    val r = list.getCellBounds(i, i)
    setRolloverIndex(list, if (r?.contains(pt) == true) i else -1)
  }

  private fun setRolloverIndex(
    list: JList<*>,
    i: Int,
  ) {
    val prev = rolloverIndex
    if (prev != i) {
      rolloverIndex = i
      // Repaint only the previous and new rollover cells
      repaintCell(list, prev)
      repaintCell(list, i)
      if (prev < 0 || i < 0) {
        // The focused (lead) cell is highlighted only when there is no rollover
        repaintCell(list, list.leadSelectionIndex)
      }
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    updateRolloverIndex(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    updateRolloverIndex(e)
  }

  override fun mouseClicked(e: MouseEvent) {
    val c = e.component
    // Only a left click on a list item (the JList selects it on mouse press)
    val isItemClicked = SwingUtilities.isLeftMouseButton(e) && rolloverIndex >= 0
    // Single click: select only, double click: select and close the menus
    val doNotClose = (c as? PopupList<*>)?.isDoNotCloseOnMouseClick == true
    val close = !doNotClose || e.clickCount >= 2
    if (isItemClicked && close && c is JList<*>) {
      // Clear the rollover state so that a stale highlight is not shown
      // when the menu is reopened
      setRolloverIndex(c, -1)
      MenuSelectionManager.defaultManager().clearSelectedPath()
    }
  }

  override fun mouseExited(e: MouseEvent) {
    (e.component as? JList<*>)?.also { setRolloverIndex(it, -1) }
  }

  companion object {
    private fun repaintCell(
      list: JList<*>,
      i: Int,
    ) {
      val r = if (i >= 0) list.getCellBounds(i, i) else null
      r?.also { list.repaint(it) }
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
