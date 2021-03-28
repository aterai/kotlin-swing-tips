package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  UIManager.put("Menu.submenuPopupOffsetX", -16)
  UIManager.put("Menu.submenuPopupOffsetY", -3)
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar() = JMenuBar().also {
  it.add(createMenu("File"))
  it.add(Box.createHorizontalGlue())
  it.add(createMenu("☰")) // \u2630
}

private fun createMenu(key: String): JMenu {
  val menu = object : JMenu(key) {
    override fun setPopupMenuVisible(b: Boolean) {
      if (isTopLevelMenu) {
        val p = location
        val r = rootPane.bounds
        val d1 = popupMenu.preferredSize
        if (p.x + d1.width > r.width) {
          val d2 = preferredSize
          setMenuLocation(d2.width - d1.width, d2.height)
        }
      }
      super.setPopupMenuVisible(b)
    }
  }
  menu.add(Box.createHorizontalStrut(200))
  val sub = createTitledMenu("Bookmarks")
  sub.add("Item 1")
  sub.add("Item 2")
  sub.add(Box.createHorizontalStrut(200))
  menu.add(sub)
  val sub2 = JMenu("submenuPopupOffsetX")
  sub2.add("Item 3")
  sub2.add("Item 4")
  menu.add(sub2)
  menu.add("Item 5")
  menu.add("Item 6")
  val sub3 = createTitledMenu("Help")
  sub3.add("Help 1")
  sub3.add("Help 2")
  sub3.add("Help 3")
  sub3.add("Help 4")
  menu.add(sub3)
  return menu
}

private fun createTitledMenu(title: String): JMenu {
  val menu = object : JMenu(title) {
    override fun setPopupMenuVisible(b: Boolean) {
      popupMenu.setPopupSize(parent.preferredSize)
      val p = location
      setMenuLocation(-p.x, -p.y)
      super.setPopupMenuVisible(b)
    }

    override fun add(item: JMenuItem): JMenuItem {
      item.maximumSize = Dimension(Short.MAX_VALUE.toInt(), item.preferredSize.height)
      return super.add(item)
    }
  }
  menu.delay = 100_000
  menu.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      (e.component as? AbstractButton)?.doClick()
      menu.popupMenu.isVisible = true
    }
  })
  val button = JButton(" < ").also {
    it.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
    it.isContentAreaFilled = false
    it.isFocusPainted = false
    it.background = Color.WHITE
    it.isOpaque = false
    it.addMouseMotionListener(object : MouseAdapter() {
      override fun mouseMoved(e: MouseEvent) {
        e.component.parent.repaint()
      }
    })
    it.addActionListener { menu.popupMenu.isVisible = false }
  }
  val titleBar = JMenuItem().also {
    it.isOpaque = true
    it.isEnabled = false
    it.isFocusable = false
    it.layout = BorderLayout(0, 0)
    it.add(button, BorderLayout.WEST)
    it.add(JLabel(title, SwingConstants.CENTER))
    it.add(Box.createHorizontalStrut(button.preferredSize.width), BorderLayout.EAST)
    it.preferredSize = Dimension(200, 24)
  }
  menu.add(titleBar)
  return menu
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
