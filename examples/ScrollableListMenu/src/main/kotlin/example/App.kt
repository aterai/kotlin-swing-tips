package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
  val model = DefaultListModel<String>()
  fonts.map { it.fontName }.forEach { model.addElement(it) }
  val list = PopupList(model)
  val scroll = JScrollPane(list)
  scroll.verticalScrollBar.unitIncrement = list.fixedCellHeight
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()
  val subMenu = JMenu("Font list")
  subMenu.add(scroll)
  val menu = JMenu("Menu")
  menu.add(subMenu)
  menu.addSeparator()
  menu.add("Item 1")
  menu.add("Item 2")
  val mb = JMenuBar()
  mb.add(menu)
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private class PopupList<E>(
  model: ListModel<E>,
) : JList<E>(model) {
  private var listener: PopupListMouseListener? = null

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    super.updateUI()
    fixedCellHeight = 20
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    listener = PopupListMouseListener()
    addMouseListener(listener)
    addMouseMotionListener(listener)
    val selectedBg = Color(0x91_C9_F7)
    val renderer = cellRenderer
    cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer
        .getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          if (it is JComponent && listener?.isRolloverIndex(index) == true) {
            it.setBackground(selectedBg)
            it.setForeground(Color.WHITE)
          }
        }
    }
  }
}

private class PopupListMouseListener : MouseAdapter() {
  private var index = -1

  fun isRolloverIndex(i: Int) = index == i

  private fun setRollover(e: MouseEvent) {
    val c = e.component
    if (c is JList<*>) {
      index = c.locationToIndex(e.point)
      c.repaint()
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    setRollover(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    setRollover(e)
  }

  override fun mouseClicked(e: MouseEvent) {
    MenuSelectionManager.defaultManager().clearSelectedPath()
  }

  override fun mouseExited(e: MouseEvent) {
    index = -1
    e.component.repaint()
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
