package example

import java.awt.*
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun createUI(): Component {
  val combo = object : JComboBox<ComboItem>(makeModel()) {
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(listener)
      super.updateUI()
      prototypeDisplayValue = ComboItem("*Create a merge commit*", "")
      setRenderer(CheckComboBoxRenderer(this))
      listener = WidePopupMenuListener()
      addPopupMenuListener(listener)
    }
  }
  return JPanel(FlowLayout(FlowLayout.LEADING)).also {
    it.add(combo)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): Array<ComboItem> = arrayOf(
  ComboItem(
    "Create a merge commit",
    """
        |All commits from this branch
        |will be added to the base branch
        |via a merge commit.
    """.trimMargin(),
  ),
  ComboItem(
    "Squash and merge",
    """
        |The 1 commit from this branch
        |will be added to the base branch.
    """.trimMargin(),
  ),
  ComboItem(
    "Rebase and merge",
    """
        |The 1 commit from this branch
        |will be rebased and added to the
        |base branch.
    """.trimMargin(),
  ),
)

private data class ComboItem(
  val title: String,
  val description: String,
) {
  override fun toString() = title
}

private class ComboItemPanel(
  data: ComboItem,
) : JPanel(BorderLayout()) {
  private val checkBox = JCheckBox()
  private val label = JLabel()
  private val textArea = JTextArea()

  init {
    setItem(data)
    checkBox.isOpaque = false
    checkBox.isFocusable = false
    val box = Box.createVerticalBox()
    box.add(checkBox)
    box.add(Box.createVerticalGlue())
    add(box, BorderLayout.WEST)

    label.font = label.font.deriveFont(Font.BOLD, 14f)
    textArea.border = BorderFactory.createEmptyBorder()
    textArea.isOpaque = false
    textArea.font = textArea.font.deriveFont(12f)
    val p = JPanel(BorderLayout())
    p.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    p.isOpaque = false
    p.add(label, BorderLayout.NORTH)
    p.add(textArea)
    add(p)
    isOpaque = false
  }

  fun setSelected(b: Boolean) {
    checkBox.isSelected = b
  }

  fun setItem(item: ComboItem) {
    label.text = item.title
    textArea.text = item.description
  }
}

private class CheckComboBoxRenderer(
  private val combo: JComboBox<ComboItem>,
) : ListCellRenderer<ComboItem> {
  private val renderer: ComboItemPanel
  private val label = JLabel()

  init {
    val proto = combo.prototypeDisplayValue ?: ComboItem("", "")
    renderer = ComboItemPanel(proto)
  }

  override fun getListCellRendererComponent(
    list: JList<out ComboItem>,
    value: ComboItem?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c: Component
    if (index >= 0 && value != null) {
      renderer.setItem(value)
      if (isSelected) {
        renderer.setSelected(true)
        renderer.isOpaque = true
        renderer.background = SELECTED_BGC
      } else {
        renderer.setSelected(combo.selectedIndex == index)
        renderer.isOpaque = false
        renderer.background = Color.WHITE
      }
      c = renderer
    } else {
      label.isOpaque = false
      label.text = value?.toString() ?: ""
      c = label
    }
    return c
  }

  companion object {
    private val SELECTED_BGC = Color(0xC0_E8_FF)
  }
}

private class WidePopupMenuListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val combo = e.source as? JComboBox<*> ?: return
    val size = combo.size
    if (size.width < POPUP_MIN_WIDTH) {
      // Temporarily widen the combo box so that BasicComboPopup#getPopupLocation()
      // sizes the popup from the widened bounds. The nested showPopup() fires this
      // listener again, but the width check above prevents infinite recursion.
      combo.setSize(POPUP_MIN_WIDTH, size.height)
      combo.showPopup()
      // The outer BasicComboPopup#show() still calls getPopupLocation() after this
      // listener returns, so restoring the size synchronously would shrink the
      // already visible popup back to the combo box width.
      EventQueue.invokeLater { combo.size = size }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }

  companion object {
    private const val POPUP_MIN_WIDTH = 260
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
