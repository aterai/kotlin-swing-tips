package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.util.BitSet
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.undo.AbstractUndoableEdit
import javax.swing.undo.UndoManager
import javax.swing.undo.UndoableEditSupport

class MainPanel : JPanel(BorderLayout()) {
  protected var status = BitSet.valueOf(longArrayOf(java.lang.Long.valueOf("111000111", 2)))
  @Transient
  protected val undoSupport = UndoableEditSupport()
  private val label = JLabel(print(status))
  private val panel = JPanel(GridLayout(0, 8))
  private val um = UndoManager()
  private val undoAction = UndoAction(um)
  private val redoAction = RedoAction(um)
  private val selectAllAction = object : AbstractAction("select all") {
    override fun actionPerformed(e: ActionEvent) {
      val newValue = BitSet(BIT_LENGTH)
      newValue.set(0, BIT_LENGTH, true)
      undoSupport.postEdit(StatusEdit(status, newValue))
      updateCheckBoxes(newValue)
    }
  }
  private val clearAllAction = object : AbstractAction("clear all") {
    override fun actionPerformed(e: ActionEvent) {
      val newValue = BitSet(BIT_LENGTH)
      undoSupport.postEdit(StatusEdit(status, newValue))
      updateCheckBoxes(newValue)
    }
  }

  init {
    undoSupport.addUndoableEditListener(um)
    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(JButton(undoAction))
    box.add(Box.createHorizontalStrut(2))
    box.add(JButton(redoAction))
    box.add(Box.createHorizontalStrut(2))
    box.add(JButton(selectAllAction))
    box.add(Box.createHorizontalStrut(2))
    box.add(JButton(clearAllAction))
    box.add(Box.createHorizontalStrut(2))

    for (i in 0 until BIT_LENGTH) {
      val c = JCheckBox(Integer.toString(i), status.get(i))
      c.addActionListener { e ->
        val v = (e.getSource() as? JCheckBox)?.isSelected() ?: false
        val newValue = status.get(0, BIT_LENGTH)
        newValue.set(i, v)
        undoSupport.postEdit(StatusEdit(status, newValue))
        status = newValue
        label.setText(print(status))
      }
      panel.add(c)
    }

    label.setFont(label.getFont().deriveFont(8f))

    add(label, BorderLayout.NORTH)
    add(JScrollPane(panel))
    add(box, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  protected fun updateCheckBoxes(value: BitSet) {
    status = value
    for (i in 0 until BIT_LENGTH) {
      (panel.getComponent(i) as? JCheckBox)?.setSelected(status.get(i))
    }
    label.setText(print(status))
  }

  inner class StatusEdit(val oldValue: BitSet, val newValue: BitSet) : AbstractUndoableEdit() {
    override fun undo() { // throws CannotUndoException {
      super.undo()
      updateCheckBoxes(oldValue)
    }

    override fun redo() { // throws CannotRedoException {
      super.redo()
      updateCheckBoxes(newValue)
    }
  }

  private fun print(bitSet: BitSet): String {
    val buf = StringBuilder()
    for (lv in bitSet.toLongArray()) {
      buf.insert(0, java.lang.Long.toUnsignedString(lv, 2))
    }
    val b = buf.toString()
    val count = bitSet.cardinality()
    return "<html>0b" + ZEROPAD.substring(b.length) + b + "<br/> count: " + count
  }

  companion object {
    // Long.MAX_VALUE
    // 0b111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111
    // const val BIT_LENGTH = 63;
    const val BIT_LENGTH = 72
    val ZEROPAD = "0".repeat(BIT_LENGTH)
  }
}

class UndoAction(private val um: UndoManager) : AbstractAction("undo") {
  override fun actionPerformed(e: ActionEvent) {
    if (um.canUndo()) {
      um.undo()
    }
  }
}

class RedoAction(private val um: UndoManager) : AbstractAction("redo") {
  override fun actionPerformed(e: ActionEvent) {
    if (um.canRedo()) {
      um.redo()
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
