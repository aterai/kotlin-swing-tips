package example

import java.awt.*
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
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
  val tags = ArrayList<String>()
  private val textField = JTextField(15)
  private val tagContainer = object : JPanel(FlowLayout(FlowLayout.LEFT)) {
    override fun updateUI() {
      super.updateUI()
      setBackground(UIManager.getColor("TextField.background"))
    }
  }

  init {
    textField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    textField.addActionListener {
      val text = textField.getText().trim()
      if (!text.isEmpty() && !tags.contains(text)) {
        addTag(text)
        textField.text = ""
      }
    }
    textField.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && textField.getText().isEmpty()) {
          removeLastTag()
          e.consume()
        }
      }
    })
    tagContainer.add(textField)
    val scroll = object : JScrollPane(tagContainer) {
      override fun updateUI() {
        super.updateUI()
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
        setBorder(BorderFactory.createEmptyBorder())
        setViewportBorder(BorderFactory.createEmptyBorder())
      }
    }
    add(scroll)
  }

  override fun updateUI() {
    super.updateUI()
    setBorder(UIManager.getBorder("TextField.border"))
    setBackground(UIManager.getColor("TextField.background"))
  }

  fun getTagContainer(): Container {
    return tagContainer
  }

  private fun addTag(text: String) {
    val tag = JPanel(BorderLayout(5, 0))
    tag.setName(text)
    tag.setBackground(Color(230, 245, 255))
    val color = UIManager.getColor("Table.selectionBackground")
    val outside = BorderFactory.createLineBorder(color)
    val inside = BorderFactory.createEmptyBorder(3, 5, 3, 5)
    tag.setBorder(BorderFactory.createCompoundBorder(outside, inside)
    )
    tags.add(text)
    tag.add(JLabel(text))
    tag.add(makeCloseButton(tag), BorderLayout.EAST)
    tagContainer.add(tag, tagContainer.componentCount - 1)
    resizeAndRepaint()
  }

  private fun makeCloseButton(tag: JPanel): JButton {
    val closeBtn = object : JButton("~") {
      override fun updateUI() {
        super.updateUI()
        setContentAreaFilled(false)
        setFocusPainted(false)
        setFocusable(false)
        setBorder(BorderFactory.createEmptyBorder())
      }
    }
    closeBtn.addActionListener {
      tags.remove(tag.getName())
      tagContainer.remove(tag)
      resizeAndRepaint()
    }
    closeBtn.addMouseListener(object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) {
        e.component.setForeground(Color.RED)
      }

      override fun mouseExited(e: MouseEvent) {
        e.component.setForeground(UIManager.getColor("Button.foreground"))
      }
    })
    return closeBtn
  }

  private fun removeLastTag() {
    val count = tagContainer.componentCount
    val moreThanOne = count > 1
    if (moreThanOne) {
      tags.removeAt(tags.count() - 1)
      tagContainer.remove(count - 2)
      resizeAndRepaint()
    }
  }

  private fun resizeAndRepaint() {
    revalidate()
    repaint()
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
