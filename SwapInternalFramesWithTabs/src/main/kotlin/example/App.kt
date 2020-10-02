package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private val cardLayout = CardLayout()
private val panel = JPanel(cardLayout)
private val desktopPane = JDesktopPane()
private val tabbedPane = JTabbedPane()
private var openFrameCount = 0
private var row = 0
private var col = 0

private fun createInternalFrame(): JInternalFrame {
  val title = "Document #${++openFrameCount}"
  val f = JInternalFrame(title, true, true, true, true)
  row += 1
  f.setSize(240, 120)
  f.setLocation(20 * row + 20 * col, 20 * row)
  f.isVisible = true
  EventQueue.invokeLater {
    val rect = desktopPane.bounds
    rect.setLocation(0, 0)
    if (!rect.contains(f.bounds)) {
      row = 0
      col += 1
    }
  }
  return f
}

fun makeUI(): Component {
  panel.add(desktopPane, desktopPane.javaClass.name)
  panel.add(tabbedPane, tabbedPane.javaClass.name)
  val swapButton = JToggleButton("JDesktopPane <-> JTabbedPane")
  swapButton.addActionListener { e ->
    if ((e.source as? AbstractButton)?.isSelected == true) {
      desktopPane.allFrames.sortedBy { it.title }
        .forEach {
          tabbedPane.addTab(it.title, it.frameIcon, it.contentPane)
        }
      desktopPane.selectedFrame?.also {
        tabbedPane.selectedIndex = tabbedPane.indexOfTab(it.title)
      }
      cardLayout.show(panel, tabbedPane.javaClass.name)
    } else {
      desktopPane.allFrames
        .forEach {
          it.contentPane = tabbedPane.getComponentAt(tabbedPane.indexOfTab(it.title)) as? Container
        }
      cardLayout.show(panel, desktopPane.javaClass.name)
    }
  }
  val addButton = JButton("add")
  addButton.addActionListener {
    val f = createInternalFrame()
    desktopPane.add(f)
    val icon = f.frameIcon
    val title = f.title
    val c = JScrollPane(JTextArea(title))
    if (desktopPane.isShowing) {
      f.add(c)
    } else {
      tabbedPane.addTab(title, icon, c)
    }
  }
  val toolbar = JToolBar("toolbar")
  toolbar.isFloatable = false
  toolbar.add(addButton)
  toolbar.add(Box.createGlue())
  toolbar.add(swapButton)
  return JPanel(BorderLayout()).also {
    it.add(panel)
    it.add(toolbar, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
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
