package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.InternalFrameEvent
import javax.swing.event.InternalFrameListener

private var openFrameCount = 0
private var row = 0
private var col = 0

fun makeUI(): Component {
  val desktop = JDesktopPane()
  val im = desktop.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape")

  val action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      desktop.selectedFrame?.also {
        desktop.desktopManager.closeFrame(it)
      }
    }
  }
  val am = desktop.actionMap
  am.put("escape", action)

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.add(createToolBar(desktop), BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBar(desktop: JDesktopPane): JToolBar {
  val toolBar = JToolBar()
  toolBar.isFloatable = false
  val cl = Thread.currentThread().contextClassLoader
  var b = JButton(ImageIcon(cl.getResource("example/icon_new-file.png")))
  b.addActionListener {
    val frame = makeInternalFrame(desktop)
    desktop.add(frame)
    runCatching {
      frame.isSelected = true
      if (openFrameCount % 2 == 0) {
        frame.isIcon = true
      }
    }
  }
  b.toolTipText = "create new InternalFrame"
  toolBar.add(b)
  toolBar.add(Box.createGlue())
  b = JButton(CloseIcon(Color.RED))
  b.addActionListener {
    desktop.selectedFrame?.dispose()
  }
  b.toolTipText = "f.dispose();"
  toolBar.add(b)
  b = JButton(CloseIcon(Color.GREEN))
  b.addActionListener {
    desktop.selectedFrame?.also {
      desktop.desktopManager.closeFrame(it)
    }
  }
  b.toolTipText = "desktop.getDesktopManager().closeFrame(f);"
  toolBar.add(b)
  b = JButton(CloseIcon(Color.BLUE))
  b.addActionListener {
    desktop.selectedFrame?.doDefaultCloseAction()
  }
  b.toolTipText = "f.doDefaultCloseAction();"
  toolBar.add(b)
  b = JButton(CloseIcon(Color.YELLOW))
  b.addActionListener {
    desktop.selectedFrame?.also {
      runCatching {
        it.isClosed = true
      }
    }
  }
  b.toolTipText = "f.setClosed(true);"
  toolBar.add(b)
  return toolBar
}

private fun makeInternalFrame(desktop: JDesktopPane): JInternalFrame {
  val f = JInternalFrame(
    "Document #${++openFrameCount}",
    true,
    true,
    true,
    true
  )
  row += 1
  f.setSize(240, 120)
  f.setLocation(20 * row + 20 * col, 20 * row)
  f.isVisible = true
  EventQueue.invokeLater {
    val rect = desktop.bounds
    rect.setLocation(0, 0)
    if (!rect.contains(f.bounds)) {
      row = 0
      col += 1
    }
  }
  f.addInternalFrameListener(TestInternalFrameListener())
  return f
}

private class TestInternalFrameListener : InternalFrameListener {
  override fun internalFrameClosing(e: InternalFrameEvent) {
    println("internalFrameClosing: " + e.internalFrame.title)
  }

  override fun internalFrameClosed(e: InternalFrameEvent) {
    println("internalFrameClosed: " + e.internalFrame.title)
  }

  override fun internalFrameOpened(e: InternalFrameEvent) {
    println("internalFrameOpened: " + e.internalFrame.title)
  }

  override fun internalFrameIconified(e: InternalFrameEvent) {
    println("internalFrameIconified: " + e.internalFrame.title)
  }

  override fun internalFrameDeiconified(e: InternalFrameEvent) {
    println("internalFrameDeiconified: " + e.internalFrame.title)
  }

  override fun internalFrameActivated(e: InternalFrameEvent) {
    // println("internalFrameActivated: " + e.getInternalFrame().getTitle())
  }

  override fun internalFrameDeactivated(e: InternalFrameEvent) {
    println("internalFrameDeactivated: " + e.internalFrame.title)
  }
}

private class CloseIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
