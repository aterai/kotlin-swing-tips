package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

fun createUI(): Component {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(1, 2, 2, 2)).also {
    it.add(JScrollPane(JTree()))
    it.add(JScrollPane(AlternateRowColorTree()))
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private class AlternateRowColorTree : JTree() {
  private var handler: RepaintHandler? = null

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = ALTERNATE_COLOR
    (0..<rowCount)
      .filter { it % 2 == 0 }
      .mapNotNull { getRowBounds(it) }
      .forEach { g2.fillRect(0, it.y, width, it.height) }
    selectionRows?.also { rows ->
      g2.paint = SELECTED_COLOR
      rows
        .asList()
        .mapNotNull { getRowBounds(it) }
        .forEach { g2.fillRect(0, it.y, width, it.height) }
    }
    // Paint the nodes only once, on top of the stripes and the selection rows
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath?.let { getPathBounds(it) }?.also { r ->
        g2.paint = SELECTED_COLOR.darker()
        g2.drawRect(0, r.y, width - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  private fun repaintWholeRow(path: TreePath?) {
    path?.let { getPathBounds(it) }?.also {
      repaint(0, it.y, width, it.height)
    }
  }

  override fun updateUI() {
    removeTreeSelectionListener(handler)
    removePropertyChangeListener(LEAD_SELECTION_PATH_PROPERTY, handler)
    removeFocusListener(handler)
    super.updateUI()
    // The selection background and the lead row border are painted across
    // the whole width of the tree, but the TreeUI repaints only the node
    // bounds, so repaint the whole row of the changed paths instead of using
    // the global UIManager.put("Tree.repaintWholeRow", true) setting.
    val h = RepaintHandler()
    addTreeSelectionListener(h)
    addPropertyChangeListener(LEAD_SELECTION_PATH_PROPERTY, h)
    addFocusListener(h)
    handler = h
    setCellRenderer(TransparentTreeCellRenderer())
    isOpaque = false
  }

  private inner class RepaintHandler :
    TreeSelectionListener,
    PropertyChangeListener,
    FocusListener {
    override fun valueChanged(e: TreeSelectionEvent) {
      e.paths?.forEach { repaintWholeRow(it) }
    }

    override fun propertyChange(e: PropertyChangeEvent) {
      repaintWholeRow(e.oldValue as? TreePath)
      repaintWholeRow(e.newValue as? TreePath)
    }

    override fun focusGained(e: FocusEvent) {
      repaintWholeRow(leadSelectionPath)
    }

    override fun focusLost(e: FocusEvent) {
      repaintWholeRow(leadSelectionPath)
    }
  }

  companion object {
    private val SELECTED_COLOR = Color(0x64_32_64_FF, true)
    private val ALTERNATE_COLOR = Color(0xCC_CC_CC)
  }
}

private class TransparentTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      false,
    )
    (c as? JComponent)?.isOpaque = false
    return c
  }

  override fun getBackgroundNonSelectionColor() = ALPHA_OF_ZERO

  override fun getBackgroundSelectionColor() = ALPHA_OF_ZERO

  companion object {
    private val ALPHA_OF_ZERO = Color(0x0, true)
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
