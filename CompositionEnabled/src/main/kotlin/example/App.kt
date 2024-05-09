package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*

private val check = JCheckBox("一時ウィンドウ(入力モード)->enterでセル編集開始")

fun makeUI(): Component {
  val table = object : JTable(4, 3) {
    override fun processKeyBinding(
      ks: KeyStroke,
      e: KeyEvent?,
      condition: Int,
      pressed: Boolean,
    ): Boolean {
      if (check.isSelected && !isTabOrEnterKey(ks)) {
        startEditing(ks, pressed)
      }
      return super.processKeyBinding(ks, e, condition, pressed)
    }

    private fun startEditing(ks: KeyStroke, pressed: Boolean) {
      val editingOrPressed = isEditing || pressed
      val isCompositionEnabled = inputContext?.isCompositionEnabled() == true
      if (isCompositionEnabled && !ks.isOnKeyRelease && !editingOrPressed) {
        val selectedRow = selectedRow
        val selectedColumn = selectedColumn
        if (selectedRow != -1 && selectedColumn != -1) {
          editCellAt(selectedRow, selectedColumn)
          // val b = editCellAt(selectedRow, selectedColumn)
          // println("editCellAt: $b")
        }
      }
    }

    protected fun isTabOrEnterKey(ks: KeyStroke) =
      KeyStroke.getKeyStroke('\t') == ks || KeyStroke.getKeyStroke('\n') == ks
  }
  table.putClientProperty("terminateEditOnFocusLost", true)

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
