package example

import java.awt.*
import java.security.AccessController
import java.security.PrivilegedAction
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  val slider0 = JSlider(0, 100, 50)
  box.add(makeTitledSeparator("Default", slider0))
  box.add(Box.createVerticalStrut(2))

  val slider1 = JSlider(0, 100, 50)
  slider1.isEnabled = false
  box.add(makeTitledSeparator("JSlider#setEnabled(false)", slider1))
  box.add(Box.createVerticalStrut(2))

  // https://community.oracle.com/thread/1360123
  val slider2 = object : JSlider(0, 100, 50) {
    override fun updateUI() {
      super.updateUI()
      val slider = this
      val pa = PrivilegedAction<Unit?> {
        runCatching {
          val uiClass = BasicSliderUI::class.java
          val uninstall = uiClass.getDeclaredMethod(
            "uninstallListeners",
            JSlider::class.java,
          )
          uninstall.isAccessible = true
          uninstall.invoke(getUI(), slider)
          val uninstallKbdActs = uiClass.getDeclaredMethod(
            "uninstallKeyboardActions",
            JSlider::class.java,
          )
          uninstallKbdActs.isAccessible = true
          uninstallKbdActs.invoke(getUI(), slider)
        }
      }
      AccessController.doPrivileged(pa)
    }
  }
  box.add(makeTitledSeparator("BasicSliderUI#uninstallListeners(...)", slider2))
  box.add(Box.createVerticalStrut(2))

  val slider3 = object : JSlider(0, 100, 50) {
    override fun updateUI() {
      super.updateUI()
      isFocusable = false // uninstallKeyboardActions
      for (l in mouseListeners) {
        removeMouseListener(l)
      }
      for (l in mouseMotionListeners) {
        removeMouseMotionListener(l)
      }
    }
  }
  box.add(makeTitledSeparator("JSlider#removeMouseListener(...)", slider3))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledSeparator(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c, BorderLayout.NORTH)
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
