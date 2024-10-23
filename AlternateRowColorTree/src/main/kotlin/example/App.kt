package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultTreeCellRenderer

fun makeUI(): Component {
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
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color(0xCC_CC_CC)
    (0..<rowCount)
      .filter { it % 2 == 0 }
      .map { getRowBounds(it) }
      .forEach { g2.fillRect(0, it.y, width, it.height) }
    val selections = selectionRows
    if (selections != null) {
      g2.paint = SELECTED_COLOR
      selections
        .map { getRowBounds(it) }
        .forEach { g2.fillRect(0, it.y, width, it.height) }
      super.paintComponent(g)
      if (hasFocus()) {
        leadSelectionPath?.also {
          val r = getRowBounds(getRowForPath(it))
          g2.paint = SELECTED_COLOR.darker()
          g2.drawRect(0, r.y, width - 1, r.height - 1)
        }
      }
    }
    super.paintComponent(g)
    g2.dispose()
  }

  override fun updateUI() {
    super.updateUI()
    UIManager.put("Tree.repaintWholeRow", java.lang.Boolean.TRUE)
    setCellRenderer(TransparentTreeCellRenderer())
    isOpaque = false
  }

  companion object {
    private val SELECTED_COLOR = Color(0x64_32_64_FF, true)
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

  override fun getBackgroundNonSelectionColor() = getBackgroundSelectionColor()

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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
