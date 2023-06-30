package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicInternalFrameUI

fun makeUI(): Component {
  val button = JButton("close")
  button.addActionListener {
    (button.topLevelAncestor as? Window)?.also {
      it.dispatchEvent(WindowEvent(it, WindowEvent.WINDOW_CLOSING))
    }
  }

  val p = JPanel(BorderLayout())
  p.add(JScrollPane(JTree()))
  p.add(button, BorderLayout.SOUTH)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  val f = DraggableInternalFrame("title")
  f.contentPane.add(p)
  f.jMenuBar = mb
  EventQueue.invokeLater { f.isVisible = true }

  return object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      background = Color(1f, 1f, 1f, .01f)
    }
  }.also {
    it.add(f)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.component)
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.location
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
  }
}

private class DraggableInternalFrame(title: String?) : JInternalFrame(title) {
  init {
    val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    focusManager.addPropertyChangeListener { e ->
      if ("activeWindow" == e.propertyName) {
        runCatching {
          setSelected(e.newValue != null)
        }
      }
    }
  }

  override fun updateUI() {
    super.updateUI()
    (ui as? BasicInternalFrameUI)?.northPane?.also { titleBar ->
      for (l in titleBar.getListeners(MouseMotionListener::class.java)) {
        titleBar.removeMouseMotionListener(l)
      }
      val dwl = DragWindowListener()
      titleBar.addMouseListener(dwl)
      titleBar.addMouseMotionListener(dwl)
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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
      isUndecorated = true
      rootPane.windowDecorationStyle = JRootPane.PLAIN_DIALOG
      background = Color(0x0, true)
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
