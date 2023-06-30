package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.Timer
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val textArea = makeTextArea()
  val handler: MouseAdapter = TextComponentMouseHandler(textArea)
  textArea.addMouseListener(handler)
  textArea.addMouseMotionListener(handler)

  val split = JSplitPane()
  split.resizeWeight = .5
  split.leftComponent = makeTitledPanel("Default", makeTextArea())
  split.rightComponent = makeTitledPanel("MouseListener", textArea)

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTextArea(): JTextComponent {
  val txt = """
    AAA BBB CCC
    AAa bbb cCC aa1111111111111111 bb2 cc3
    aa bb cc
    11 22 33
  """.trimIndent()
  val textArea = JTextArea(txt)
  textArea.lineWrap = true
  textArea.componentPopupMenu = TextFieldPopupMenu()
  return textArea
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.add(JLabel(title), BorderLayout.NORTH)
  p.add(JScrollPane(c))
  return p
}

private class TextComponentMouseHandler(textArea: JTextComponent) : MouseAdapter() {
  private val count = AtomicInteger(0)
  private val holdTimer = Timer(1000, null)
  private val list = listOf(
    DefaultEditorKit.selectWordAction,
    DefaultEditorKit.selectLineAction,
    DefaultEditorKit.selectParagraphAction,
    DefaultEditorKit.selectAllAction
  )

  init {
    holdTimer.initialDelay = 500
    holdTimer.addActionListener { e ->
      val timer = e.source as? Timer
      if (timer != null && timer.isRunning) {
        val i = count.getAndIncrement()
        if (i < list.size) {
          val cmd = list[i]
          textArea.actionMap[cmd].actionPerformed(e)
        } else {
          timer.stop()
          count.set(0)
        }
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val textArea = e.component as? JTextComponent ?: return
    val isSingleClick = e.clickCount == 1
    if (isSingleClick) {
      if (!textArea.hasFocus()) {
        textArea.requestFocusInWindow()
      }
      if (textArea.selectedText == null) {
        // Java 9: val pos = textArea.viewToModel2D(e.point)
        val pos = textArea.viewToModel(e.point)
        textArea.caretPosition = pos
      }
      if (e.button == MouseEvent.BUTTON1) {
        holdTimer.start()
      }
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    holdTimer.stop()
    count.set(0)
  }

  override fun mouseDragged(e: MouseEvent) {
    mouseReleased(e)
  }
}

private class TextFieldPopupMenu : JPopupMenu() {
  init {
    add(DefaultEditorKit.CutAction())
    add(DefaultEditorKit.CopyAction())
    add(DefaultEditorKit.PasteAction())
    add("delete").addActionListener {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText == null
      for (menuElement in subElements) {
        val m = menuElement.component
        if (m is JMenuItem && m.action is DefaultEditorKit.PasteAction) {
          continue
        }
        m.isEnabled = hasSelectedText
      }
      super.show(c, x, y)
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
