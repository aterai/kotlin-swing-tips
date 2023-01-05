package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.CompoundBorder
import javax.swing.plaf.UIResource
import javax.swing.plaf.basic.BasicArrowButton
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup

fun makeUI(): Component {
  val items = arrayOf(
    "<html><font color='red'>Sunday</font> <font color='gray'>(Sun.)",
    "<html><font color='black'>Monday</font> <font color='gray'>(Mon.)",
    "<html><font color='black'>Tuesday</font> <font color='gray'>(Tue.)",
    "<html><font color='black'>Wednesday</font> <font color='gray'>(Wed.)",
    "<html><font color='black'>Thursday</font> <font color='gray'>(Thu.)",
    "<html><font color='black'>Friday</font> <font color='gray'>(Fri.)",
    "<html><font color='blue'>Saturday</font> <font color='gray'>(Sat.)"
  )
  val p1 = JPanel(BorderLayout(5, 5))
  p1.add(JSpinner(SpinnerListModel(items)))
  p1.border = BorderFactory.createTitledBorder("JSpinner")

  val p2 = JPanel(BorderLayout(5, 5))
  p2.add(makeColorSpinner(items))
  p2.border = BorderFactory.createTitledBorder("ColorSpinner(JComboBox)")

  val panel = JPanel(BorderLayout(25, 25))
  panel.add(p1, BorderLayout.NORTH)
  panel.add(p2, BorderLayout.SOUTH)
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(panel, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun <E> makeColorSpinner(items: Array<E>): Component {
  UIManager.put("ComboBox.squareButton", false)
  val comboBox = object : JComboBox<E>(items) {
    override fun updateUI() {
      super.updateUI()
      setUI(NoPopupComboBoxUI())
      isFocusable = false
      val r = getRenderer()
      setRenderer { list, value, index, _, _ ->
        r.getListCellRendererComponent(list, value, index, false, false).also {
          (it as? JComponent)?.border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
        }
      }
    }
  }

  val nb = createArrowButton(SwingConstants.NORTH)
  nb.addActionListener { e ->
    e.source = comboBox
    comboBox.actionMap["selectPrevious2"].actionPerformed(e)
  }

  val sb = createArrowButton(SwingConstants.SOUTH)
  sb.addActionListener { e ->
    e.source = comboBox
    comboBox.actionMap["selectNext2"].actionPerformed(e)
  }

  val box = Box.createVerticalBox()
  box.add(nb)
  box.add(sb)

  val p = object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.height = 20
    }
  }
  p.add(comboBox)
  p.add(box, BorderLayout.EAST)
  return p
}

private fun createArrowButton(direction: Int) = object : BasicArrowButton(direction) {
  override fun updateUI() {
    super.updateUI()
    val buttonBorder = UIManager.getBorder("Spinner.arrowButtonBorder")
    border = if (buttonBorder is UIResource) {
      CompoundBorder(buttonBorder, null)
    } else {
      buttonBorder
    }
    inheritsPopupMenu = true
  }
}

private class NoPopupComboBoxUI : BasicComboBoxUI() {
  override fun createArrowButton() = JButton().also {
    it.border = BorderFactory.createEmptyBorder()
    it.isVisible = false
  }

  override fun createPopup() = object : BasicComboPopup(comboBox) {
    override fun show() {
      // println("disable togglePopup")
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
