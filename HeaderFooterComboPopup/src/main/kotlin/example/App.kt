package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup

fun makeUI(): Component {
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
  private var header: JLabel? = null
  private var footer: JMenuItem? = null

  override fun configurePopup() {
    super.configurePopup()
    configureHeader()
    configureFooter()
    add(header, 0)
    add(footer)
  }

  private fun configureHeader() {
    header = JLabel("History").also {
      it.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
      it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), 24)
      it.alignmentX = Component.CENTER_ALIGNMENT
    }
  }

  private fun configureFooter() {
    val modifiers = InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
    footer = JMenuItem("Show All Bookmarks").also {
      it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_B, modifiers)
      it.addActionListener {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(invoker), "Bookmarks")
      }
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
