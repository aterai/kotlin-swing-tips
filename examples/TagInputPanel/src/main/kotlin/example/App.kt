package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Collections
import javax.swing.*

fun createUI(): Component {
  val log = JTextArea()
  val tagInput = TagInputPanel()
  tagInput.getTagContainer().addContainerListener(object : ContainerListener {
    override fun componentAdded(e: ContainerEvent) {
      log.text = tagInput.tags.joinToString(", ")
    }

    override fun componentRemoved(e: ContainerEvent) {
      log.text = tagInput.tags.joinToString(", ")
    }
  })
  return JPanel(BorderLayout(5, 5)).also {
    it.add(tagInput, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TagInputPanel : JPanel(BorderLayout()) {
  private val tagList = mutableListOf<String>()

  // Return a read-only view so that callers cannot desynchronize it from the tag components
  val tags: List<String> = Collections.unmodifiableList(tagList)
  private val textField = JTextField(15)
  private val tagContainer = object : JPanel(WrapFlowLayout(FlowLayout.LEFT, 5, 5)) {
    override fun updateUI() {
      super.updateUI()
      setBackground(UIManager.getColor("TextField.background"))
    }
  }

  init {
    textField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    textField.addActionListener {
      val text = textField.getText().trim()
      if (text.isNotEmpty() && !tags.contains(text)) {
        addTag(text)
        textField.text = ""
      }
    }
    textField.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val b = e.getKeyCode() == KeyEvent.VK_BACK_SPACE
        if (b && textField.getText().isEmpty()) {
          removeLastTag()
          e.consume()
        }
      }
    })
    tagContainer.add(textField)
    tagContainer.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        // The preferred height depends on the current width, so lay out
        // the ancestors again after the width has changed
        e.component.revalidate()
      }
    })
    add(tagContainer)
  }

  override fun updateUI() {
    super.updateUI()
    setBorder(UIManager.getBorder("TextField.border"))
    setBackground(UIManager.getColor("TextField.background"))
  }

  fun getTagContainer() = tagContainer

  private fun addTag(text: String) {
    val tag = JPanel(BorderLayout(5, 0))
    tag.setName(text)
    tag.setBackground(Color(230, 245, 255))
    val color = UIManager.getColor("Table.selectionBackground")
    val outside = BorderFactory.createLineBorder(color)
    val inside = BorderFactory.createEmptyBorder(3, 5, 3, 5)
    tag.setBorder(BorderFactory.createCompoundBorder(outside, inside))
    tagList.add(text)
    tag.add(JLabel(text))
    tag.add(createCloseButton(tag), BorderLayout.EAST)
    tagContainer.add(tag, tagContainer.componentCount - 1)
    resizeAndRepaint()
  }

  private fun createCloseButton(tag: JPanel): JButton {
    val closeButton = object : JButton("×") {
      override fun updateUI() {
        super.updateUI()
        setContentAreaFilled(false)
        setFocusPainted(false)
        setFocusable(false)
        setBorder(BorderFactory.createEmptyBorder())
      }
    }
    closeButton.addActionListener {
      tagList.remove(tag.getName())
      tagContainer.remove(tag)
      resizeAndRepaint()
    }
    closeButton.addMouseListener(object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) {
        e.component.setForeground(Color.RED)
      }

      override fun mouseExited(e: MouseEvent) {
        e.component.setForeground(UIManager.getColor("Button.foreground"))
      }
    })
    return closeButton
  }

  private fun removeLastTag() {
    if (tagList.isNotEmpty()) {
      // The tag components are kept in the same order as the tags list,
      // followed by the JTextField, so the last tag shares its index
      val lastIndex = tagList.size - 1
      tagList.removeAt(lastIndex)
      tagContainer.remove(lastIndex)
      resizeAndRepaint()
    }
  }

  private fun resizeAndRepaint() {
    revalidate()
    repaint()
  }
}

private class WrapFlowLayout(
  align: Int,
  hgap: Int,
  vgap: Int,
) : FlowLayout(align, hgap, vgap) {
  override fun preferredLayoutSize(target: Container): Dimension {
    synchronized(target.treeLock) {
      // Before the first layout the width is unknown, so fall back to a single row
      return if (target.width == 0) {
        super.preferredLayoutSize(target)
      } else {
        wrappedLayoutSize(target)
      }
    }
  }

  private fun wrappedLayoutSize(target: Container): Dimension {
    val targetWidth = target.width
    val insets = target.insets
    // Wrap rows with the same rule as FlowLayout#layoutContainer(...)
    val maxWidth = targetWidth - insets.left - insets.right - hgap * 2
    var height = insets.top + insets.bottom + vgap
    var rowWidth = 0
    var rowHeight = 0
    for (c in target.components) {
      if (c.isVisible) {
        val d = c.preferredSize
        if (rowWidth > 0 && rowWidth + d.width > maxWidth) {
          height += rowHeight + vgap
          rowWidth = 0
          rowHeight = 0
        }
        rowWidth += if (rowWidth > 0) hgap + d.width else d.width
        rowHeight = maxOf(rowHeight, d.height)
      }
    }
    height += rowHeight + vgap
    return Dimension(targetWidth, height)
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
