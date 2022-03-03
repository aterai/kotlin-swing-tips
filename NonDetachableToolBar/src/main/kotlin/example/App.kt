package example

import com.sun.java.swing.plaf.windows.WindowsToolBarUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val detachable = JCheckBox("Floating(detachable)", false)

  val button = JButton("button")
  button.isFocusable = false

  val toolBar = object : JToolBar() {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsToolBarUI) {
        object : WindowsToolBarUI() {
          override fun setFloating(b: Boolean, p: Point?) {
            if (detachable.isSelected) {
              super.setFloating(b, p)
            } else {
              super.setFloating(false, p)
            }
          }
        }
      } else {
        object : BasicToolBarUI() {
          override fun setFloating(b: Boolean, p: Point?) {
            if (detachable.isSelected) {
              super.setFloating(b, p)
            } else {
              super.setFloating(false, p)
            }
          }
        }
      }
      setUI(tmp)
    }
  }
  toolBar.add(JLabel("label"))
  toolBar.add(Box.createRigidArea(Dimension(5, 5)))
  toolBar.add(button)
  toolBar.add(Box.createRigidArea(Dimension(5, 5)))
  toolBar.add(JComboBox(makeModel()))
  toolBar.add(Box.createGlue())

  val movable = JCheckBox("Floatable(movable)", true)
  movable.addActionListener { e ->
    toolBar.isFloatable = (e.source as? JCheckBox)?.isSelected == true
  }

  val p = JPanel()
  p.add(movable)
  p.add(detachable)

  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("1111111")
  model.addElement("22222")
  model.addElement("3333333333333333")
  model.addElement("44444444444")
  return model
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
