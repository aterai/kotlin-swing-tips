package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

fun makeUI(): Component {
  val slider1 = JSlider(0, 100, 0)
  initSlider(slider1)

  val slider2 = object : JSlider(0, 100, 0) {
    @Transient private var listener: FocusListener? = null

    override fun updateUI() {
      removeFocusListener(listener)
      super.updateUI()
      val bgc = background
      listener = object : FocusListener {
        override fun focusGained(e: FocusEvent) {
          background = bgc.brighter()
        }

        override fun focusLost(e: FocusEvent) {
          background = bgc
        }
      }
      addFocusListener(listener)

      if (getUI() is WindowsSliderUI) {
        val wui = object : WindowsSliderUI(this) {
          override fun paintFocus(g: Graphics) {
            // empty paint
          }
        }
        setUI(wui)
      }
    }
  }
  initSlider(slider2)

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(20))
  box.add(makeTitledPanel("Default", slider1))
  box.add(Box.createVerticalStrut(20))
  box.add(makeTitledPanel("Override SliderUI#paintFocus(...)", slider2))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initSlider(slider: JSlider) {
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 5
  slider.paintTicks = true
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
