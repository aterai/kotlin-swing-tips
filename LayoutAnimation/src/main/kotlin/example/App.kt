package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.Timer

private val tree = JTree()
private val field = JTextField("", 10)
private val button = JButton("Find Next")
private val showHideButton = JButton()
private var animator: Timer? = null
private var isHidden = true

private val layout = object : BorderLayout(5, 5) {
  private var controlsHeight = 0
  private var defaultHeight = 0

  override fun preferredLayoutSize(target: Container): Dimension {
    val ps = super.preferredLayoutSize(target)
    defaultHeight = ps.height
    animator?.also {
      if (isHidden) {
        if (target.height < defaultHeight) {
          controlsHeight += 5
        }
      } else {
        if (target.height > 0) {
          controlsHeight -= 5
        }
      }
      if (controlsHeight <= 0) {
        controlsHeight = 0
        it.stop()
      } else if (controlsHeight >= defaultHeight) {
        controlsHeight = defaultHeight
        it.stop()
      }
    }
    ps.height = controlsHeight
    return ps
  }
}
private val controls = JPanel(layout)

fun makeUI(): Component {
  button.isFocusable = false
  controls.border = BorderFactory.createTitledBorder("Search down")
  controls.add(JLabel("Find what:"), BorderLayout.WEST)
  controls.add(field)
  controls.add(button, BorderLayout.EAST)
  val act = object : AbstractAction("Show/Hide Search Box") {
    override fun actionPerformed(ev: ActionEvent) {
      if (animator?.isRunning == true) {
        return
      }
      isHidden = controls.height == 0
      animator = Timer(5) { controls.revalidate() }.also {
        it.start()
      }
    }
  }
  showHideButton.action = act
  showHideButton.isFocusable = false
  val p = JPanel(BorderLayout())
  val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
  // Java 10: val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
  val im = p.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifiers), "open-search-box")
  p.actionMap.put("open-search-box", act)
  p.add(controls, BorderLayout.NORTH)
  p.add(JScrollPane(tree))
  p.add(showHideButton, BorderLayout.SOUTH)
  p.preferredSize = Dimension(320, 240)
  return p
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
