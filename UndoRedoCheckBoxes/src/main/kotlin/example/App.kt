package example

import java.awt.*
import java.awt.event.ActionEvent
import java.math.BigInteger
import javax.swing.*
import javax.swing.undo.AbstractUndoableEdit
import javax.swing.undo.UndoManager
import javax.swing.undo.UndoableEditSupport

private const val BIT_LENGTH = 50
private val ONE_PAD = "1".repeat(BIT_LENGTH)
private val ZERO_PAD = "0".repeat(BIT_LENGTH)
private var status = BigInteger("111000111", 2)
private val undoSupport = UndoableEditSupport()
private val label = JLabel(print(status))
private val panel = JPanel()
private val um = UndoManager()
private val undoAction = UndoAction(um)
private val redoAction = RedoAction(um)
private val selectAllAction = object : AbstractAction("select all") {
  override fun actionPerformed(e: ActionEvent) {
    val newValue = BigInteger(ONE_PAD, 2)
    undoSupport.postEdit(StatusEdit(status, newValue))
    updateCheckBoxes(newValue)
  }
}
private val clearAllAction = object : AbstractAction("clear all") {
  override fun actionPerformed(e: ActionEvent) {
    val newValue = BigInteger.ZERO
    undoSupport.postEdit(StatusEdit(status, newValue))
    updateCheckBoxes(newValue)
  }
}

fun makeUI(): Component {
  undoSupport.addUndoableEditListener(um)
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  for (a in listOf(undoAction, redoAction, selectAllAction, clearAllAction)) {
    box.add(JButton(a))
    box.add(Box.createHorizontalStrut(2))
  }
  for (i in 0 until BIT_LENGTH) {
    panel.add(makeCheckBox(i))
  }
  return JPanel(BorderLayout()).also {
    it.add(label, BorderLayout.NORTH)
    it.add(panel)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeCheckBox(idx: Int): JCheckBox {
  val l = BigInteger.ONE.shiftLeft(idx)
  val c = JCheckBox((idx + 1).toString(), status.and(l) != BigInteger.ZERO)
  c.addActionListener { e ->
    val cb = e.source as? JCheckBox
    val newValue = if (cb?.isSelected == true) status.or(l) else status.xor(l)
    undoSupport.postEdit(StatusEdit(status, newValue))
    status = newValue
    label.text = print(status)
  }
  return c
}

private fun updateCheckBoxes(value: BigInteger) {
  status = value
  for (i in 0 until BIT_LENGTH) {
    val l = BigInteger.ONE.shiftLeft(i)
    (panel.getComponent(i) as? JCheckBox)?.also {
      it.isSelected = status.and(l) != BigInteger.ZERO
    }
  }
  label.text = print(status)
}

private class StatusEdit(
  private val oldValue: BigInteger,
  private val newValue: BigInteger
) : AbstractUndoableEdit() {
  override fun undo() {
    super.undo()
    updateCheckBoxes(oldValue)
  }

  override fun redo() {
    super.redo()
    updateCheckBoxes(newValue)
  }
}

private fun print(l: BigInteger): String {
  val b = l.toString(2)
  val count = l.bitCount()
  return "<html>0b" + ZERO_PAD.substring(b.length) + b + "<br/> count: " + count
}

private class UndoAction(private val um: UndoManager) : AbstractAction("undo") {
  override fun actionPerformed(e: ActionEvent) {
    if (um.canUndo()) {
      um.undo()
    }
  }
}

private class RedoAction(private val um: UndoManager) : AbstractAction("redo") {
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
