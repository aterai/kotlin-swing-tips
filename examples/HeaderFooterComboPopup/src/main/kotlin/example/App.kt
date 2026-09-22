package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup

fun createUI(): Component {
  val model = arrayOf("aaa", "bbb", "ccc", "ddd", "eee", "fff", "ggg")
  val combo = object : JComboBox<String>(model) {
    override fun updateUI() {
      super.updateUI()
      val ui2 = if (ui is WindowsComboBoxUI) {
        object : WindowsComboBoxUI() {
          override fun createPopup() = HeaderFooterComboPopup(comboBox)
        }
      } else {
        object : BasicComboBoxUI() {
          override fun createPopup() = HeaderFooterComboPopup(comboBox)
        }
      }
      setUI(ui2)
      maximumRowCount = 4
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(combo, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(10, 10, 0, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HeaderFooterComboPopup(
  combo: JComboBox<Any>,
) : BasicComboPopup(combo) {
  override fun configurePopup() {
    // BasicComboPopup#configurePopup() sets a vertical BoxLayout
    // and adds the scroller that wraps the list.
    super.configurePopup()
    add(createHeader(), 0)
    add(createFooter())
  }

  private fun createHeader(): JComponent {
    val header = JLabel("History", SwingConstants.CENTER)
    header.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
    // The JLabel constructor sets LEFT_ALIGNMENT; match the CENTER_ALIGNMENT
    // of the scroller and the footer so the BoxLayout does not shift it.
    header.alignmentX = Component.CENTER_ALIGNMENT
    // A JLabel does not stretch in a BoxLayout unless its maximum
    // width is unbounded.
    val height = header.preferredSize.height
    header.maximumSize = Dimension(Short.MAX_VALUE.toInt(), height)
    return header
  }

  private fun createFooter(): JComponent {
    val modifiers = InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
    val footer = JMenuItem("Show All Bookmarks")
    footer.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_B, modifiers)
    footer.addActionListener {
      val w = SwingUtilities.getWindowAncestor(comboBox)
      JOptionPane.showMessageDialog(w, "Bookmarks")
    }
    return footer
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
