package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.UndoableEditEvent
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter
import javax.swing.text.PlainDocument
import javax.swing.undo.CompoundEdit
import javax.swing.undo.UndoManager

class MainPanel : JPanel(BorderLayout()) {
  init {
    val undoManager0 = UndoManager()
    val textField0 = JTextField("default")
    textField0.getDocument().addUndoableEditListener(undoManager0)
    val undoManager1 = UndoManager()
    val textField1 = JTextField()
    textField1.setDocument(CustomUndoPlainDocument())
    textField1.setText("111111111111111111")
    textField1.getDocument().addUndoableEditListener(undoManager1)
    val undoManager2 = DocumentFilterUndoManager()
    val textField2 = JTextField()
    textField2.setText("2222222222222222")
    val doc = textField2.getDocument()
    if (doc is AbstractDocument) {
      doc.addUndoableEditListener(undoManager2)
      doc.setDocumentFilter(undoManager2.documentFilter)
    }
    val button = JButton("setText(LocalDateTime.now(...))")
    button.addActionListener {
      val str = LocalDateTime.now(ZoneId.systemDefault()).toString()
      listOf(textField0, textField1, textField2).forEach { it.setText(str) }
    }
    val undoAction: Action = object : AbstractAction("undo") {
      override fun actionPerformed(e: ActionEvent) {
        listOf(undoManager0, undoManager1, undoManager2)
          .filter { it.canUndo() }
          .forEach { it.undo() }
      }
    }
    val redoAction: Action = object : AbstractAction("redo") {
      override fun actionPerformed(e: ActionEvent) {
        listOf(undoManager0, undoManager1, undoManager2)
          .filter { it.canRedo() }
          .forEach { it.redo() }
      }
    }
    val p = JPanel()
    p.add(JButton(undoAction))
    p.add(JButton(redoAction))
    p.add(button)
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    box.add(makeTitledPanel("Default", textField0))
    box.add(Box.createVerticalStrut(10))
    box.add(makeTitledPanel("Document#replace()+AbstractDocument#fireUndoableEditUpdate()", textField1))
    box.add(Box.createVerticalStrut(10))
    box.add(makeTitledPanel("DocumentFilter#replace()+UndoableEditListener#undoableEditHappened()", textField2))
    add(box, BorderLayout.NORTH)
    add(p, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }
}

class CustomUndoPlainDocument : PlainDocument() {
  private var compoundEdit: CompoundEdit? = null

  override fun fireUndoableEditUpdate(e: UndoableEditEvent) {
    compoundEdit?.addEdit(e.getEdit()) ?: super.fireUndoableEditUpdate(e)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    offset: Int,
    length: Int,
    text: String,
    attrs: AttributeSet?
  ) {
    if (length == 0) { // System.out.println("insert");
      super.replace(offset, length, text, attrs)
    } else {
      val ce = CompoundEdit()
      compoundEdit = ce
      super.replace(offset, length, text, attrs)
      ce.end()
      super.fireUndoableEditUpdate(UndoableEditEvent(this, compoundEdit))
      compoundEdit = null
    }
  }
}

class DocumentFilterUndoManager : UndoManager() {
  var compoundEdit: CompoundEdit? = null
  @Transient
  val documentFilter: DocumentFilter = object : DocumentFilter() {
    @Throws(BadLocationException::class)
    override fun replace(
      fb: FilterBypass,
      offset: Int,
      length: Int,
      text: String,
      attrs: AttributeSet?
    ) {
      if (length == 0) {
        fb.insertString(offset, text, attrs)
      } else {
        val ce = CompoundEdit()
        compoundEdit = ce
        fb.replace(offset, length, text, attrs)
        ce.end()
        addEdit(compoundEdit)
        compoundEdit = null
      }
    }
  }

  override fun undoableEditHappened(e: UndoableEditEvent) {
    (compoundEdit ?: this).addEdit(e.getEdit())
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
