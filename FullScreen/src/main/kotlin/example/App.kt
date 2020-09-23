package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(BorderLayout())
  p.isFocusable = true
  val ml = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount >= 2) {
        toggleFullScreenWindow(p)
      }
    }
  }
  p.addMouseListener(ml)

  val key = "full-screen"
  p.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), key)
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      toggleFullScreenWindow(p)
    }
  }
  p.actionMap.put(key, a1)

  p.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close")
  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      println("ESC KeyEvent:")
      (p.topLevelAncestor as? JDialog)?.also {
        it.dispatchEvent(WindowEvent(it, WindowEvent.WINDOW_CLOSING))
      }
    }
  }
  p.actionMap.put("close", a2)

  p.add(JLabel("<html>F11 or Double Click: toggle full-screen<br>ESC: exit"), BorderLayout.NORTH)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

fun toggleFullScreenWindow(c: JComponent) {
  (c.topLevelAncestor as? JDialog)?.also {
    val graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    if (graphicsDevice.fullScreenWindow == null) {
      it.dispose() // destroy the native resources
      it.isUndecorated = true
      it.isVisible = true // rebuilding the native resources
      graphicsDevice.fullScreenWindow = it
    } else {
      graphicsDevice.fullScreenWindow = null
      it.dispose()
      it.isUndecorated = false
      it.isVisible = true
      it.repaint()
    }
  }
  c.requestFocusInWindow() // for Ubuntu
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    val wl = object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        println("windowClosing:")
        println("  triggered only when you click on the X")
        println("  or on the close menu item in the window's system menu.'")
      }

      override fun windowClosed(e: WindowEvent) {
        println("windowClosed & rebuild:")
      }
    }
    JDialog().apply {
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      addWindowListener(wl)
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
