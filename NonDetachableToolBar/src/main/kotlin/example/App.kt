package example

import com.sun.java.swing.plaf.windows.WindowsToolBarUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val detachable = JCheckBox("Floating(detachable)", false)

  val button = JButton("button")
  button.isFocusable = false

  val toolbar = object : JToolBar("toolbar") {
    override fun updateUI() {
      super.updateUI()
      if (getUI() is WindowsToolBarUI) {
        setUI(object : WindowsToolBarUI() {
          override fun setFloating(b: Boolean, p: Point?) {
            if (detachable.isSelected) {
              super.setFloating(b, p)
            } else {
              super.setFloating(false, p)
            }
          }
        })
      } else {
        setUI(object : BasicToolBarUI() {
          override fun setFloating(b: Boolean, p: Point?) {
            if (detachable.isSelected) {
              super.setFloating(b, p)
            } else {
              super.setFloating(false, p)
            }
          }
        })
      }
    }
  }
  toolbar.add(JLabel("label"))
  toolbar.add(Box.createRigidArea(Dimension(5, 5)))
  toolbar.add(button)
  toolbar.add(Box.createRigidArea(Dimension(5, 5)))
  toolbar.add(JComboBox(makeModel()))
  toolbar.add(Box.createGlue())

  val movable = JCheckBox("Floatable(movable)", true)
  movable.addActionListener { e -> toolbar.isFloatable = (e.source as? JCheckBox)?.isSelected == true }

  val p = JPanel()
  p.add(movable)
  p.add(detachable)

  return JPanel(BorderLayout()).also {
    it.add(toolbar, BorderLayout.NORTH)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
