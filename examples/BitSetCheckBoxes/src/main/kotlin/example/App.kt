package example

import java.awt.*
import java.awt.event.ActionEvent
import java.util.BitSet
import javax.swing.*
import javax.swing.undo.AbstractUndoableEdit
import javax.swing.undo.UndoManager
import javax.swing.undo.UndoableEditSupport

private const val BIT_LENGTH = 72
private val ZERO_PAD = "0".repeat(BIT_LENGTH)
private var status = BitSet.valueOf(longArrayOf("111000111".toLong(2)))
private val undoSupport = UndoableEditSupport()
private val label = JLabel(print(status))
private val panel = JPanel(GridLayout(0, 8))
private val um = UndoManager()
private val selectAllAction = object : AbstractAction("select all") {
  override fun actionPerformed(e: ActionEvent) {
    val newValue = BitSet(BIT_LENGTH)
    newValue[0, BIT_LENGTH] = true
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

fun makeUI(): Component {
  undoSupport.addUndoableEditListener(um)
  val box = Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(JButton(UndoAction(um)))
    it.add(Box.createHorizontalStrut(2))
    it.add(JButton(RedoAction(um)))
    it.add(Box.createHorizontalStrut(2))
    it.add(JButton(selectAllAction))
    it.add(Box.createHorizontalStrut(2))
    it.add(JButton(clearAllAction))
    it.add(Box.createHorizontalStrut(2))
  }

  for (i in 0..<BIT_LENGTH) {
    val c = JCheckBox(i.toString(), status[i])
    c.addActionListener { e ->
      val v = (e.source as? JCheckBox)?.isSelected ?: false
      val newValue = status[0, BIT_LENGTH]
      newValue[i] = v
      undoSupport.postEdit(StatusEdit(status, newValue))
      status = newValue
      label.text = print(status)
    }
    panel.add(c)
  }

  label.font = label.font.deriveFont(8f)

  return JPanel(BorderLayout()).also {
    it.add(label, BorderLayout.NORTH)
    it.add(JScrollPane(panel))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateCheckBoxes(value: BitSet) {
  status = value
  for (i in 0..<BIT_LENGTH) {
    (panel.getComponent(i) as? JCheckBox)?.isSelected = status[i]
  }
  label.text = print(status)
}

private class StatusEdit(
  private val oldValue: BitSet,
  private val newValue: BitSet,
) : AbstractUndoableEdit() {
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
    buf.insert(0, lv.toULong().toString(2))
  }
  val b = buf.toString()
  val count = bitSet.cardinality()
  return "<html>0b" + ZERO_PAD.substring(b.length) + b + "<br/> count: " + count
}

private class UndoAction(
  private val um: UndoManager,
) : AbstractAction("undo") {
  override fun actionPerformed(e: ActionEvent) {
    if (um.canUndo()) {
      um.undo()
    }
  }
}

private class RedoAction(
  private val um: UndoManager,
) : AbstractAction("redo") {
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
