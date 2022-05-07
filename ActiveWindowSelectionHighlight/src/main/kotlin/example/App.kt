package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter.HighlightPainter

fun makeUI() = JSplitPane().also {
  it.resizeWeight = .5
  it.leftComponent = makeTabbedPane()
  it.rightComponent = makeTabbedPane()
  it.preferredSize = Dimension(320, 240)
}

private fun makeTabbedPane(): Component {
  val tabs = JTabbedPane()
  tabs.addChangeListener {
    requestFocusForVisibleComponent(tabs)
  }
  tabs.addFocusListener(object : FocusAdapter() {
    override fun focusGained(e: FocusEvent) {
      requestFocusForVisibleComponent(tabs)
    }
  })
  tabs.add("Custom", makeTextArea(true))
  tabs.addTab("Default", makeTextArea(false))
  return tabs
}

fun requestFocusForVisibleComponent(tabs: JTabbedPane) {
  val cmd = "requestFocusForVisibleComponent"
  val a = ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, cmd)
  EventQueue.invokeLater { tabs.actionMap[cmd].actionPerformed(a) }
}

private fun makeTextArea(flg: Boolean): Component {
  val textArea = object : JTextArea() {
    override fun updateUI() {
      caret = null
      super.updateUI()
      if (flg) {
        val oldCaret = caret
        val blinkRate = oldCaret.blinkRate
        val caret = FocusOwnerCaret()
        caret.blinkRate = blinkRate
        setCaret(caret)
        caret.isSelectionVisible = true
      }
    }
  }
  textArea.text = "FocusOwnerCaret: $flg\n111\n22222\n33333333\n"
  textArea.selectAll()
  return JScrollPane(textArea)
}

private class FocusOwnerCaret : DefaultCaret() {
  override fun focusLost(e: FocusEvent) {
    super.focusLost(e)
    updateSelectionHighlightPainter()
  }

  override fun focusGained(e: FocusEvent) {
    super.focusGained(e)
    updateSelectionHighlightPainter()
  }

  private fun updateSelectionHighlightPainter() {
    isSelectionVisible = false // removeHighlight
    isSelectionVisible = true // addHighlight
  }

  override fun getSelectionPainter(): HighlightPainter {
    val c = component
    val w = c.topLevelAncestor
    val isActive = w is Window && w.isActive
    return if (c.hasFocus() && isActive) super.getSelectionPainter() else NO_FOCUS_PAINTER
  }

  companion object {
    private val NO_FOCUS_PAINTER = DefaultHighlightPainter(Color.GRAY.brighter())
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
