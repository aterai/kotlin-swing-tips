package example

import java.awt.*
import java.awt.event.*
import java.beans.PropertyVetoException
import java.util.*
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
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  val f = DraggableInternalFrame("title")
  f.contentPane.add(p)
  f.jMenuBar = mb
  f.isVisible = true

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
      val pt = c.getLocation()
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
  }
}

private class DraggableInternalFrame(title: String?) : JInternalFrame(title) {
  override fun updateUI() {
    super.updateUI()
    val ui = getUI() as BasicInternalFrameUI
    val titleBar: Component = ui.northPane
    for (l in titleBar.getListeners(MouseMotionListener::class.java)) {
      titleBar.removeMouseMotionListener(l)
    }
    val dwl = DragWindowListener()
    titleBar.addMouseListener(dwl)
    titleBar.addMouseMotionListener(dwl)
  }

  init {
    val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    focusManager.addPropertyChangeListener { e ->
      val prop = e.propertyName
      if ("activeWindow" == prop) {
        try {
          setSelected(Objects.nonNull(e.newValue))
        } catch (ex: PropertyVetoException) {
          throw IllegalStateException(ex)
        }
      }
    }
  }
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener {
      val m = lafRadioGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
    }
    lafRadioGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
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
