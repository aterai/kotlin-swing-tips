package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.plaf.IconUIResource
import javax.swing.table.DefaultTableModel

fun createUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true

  val clearButton = JButton("clear SortKeys")
  clearButton.addActionListener {
    table.rowSorter.sortKeys = null
  }

  return JPanel(BorderLayout()).also {
    it.add(createRadioPane(table), BorderLayout.NORTH)
    it.add(clearButton, BorderLayout.SOUTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createRadioPane(table: JTable): Box {
  val group = ButtonGroup()
  val handler = ItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      val name = group.getSelection().actionCommand
      val type = SortIconType.valueOf(name)
      UIManager.put("Table.ascendingSortIcon", type.ascendingIcon)
      UIManager.put("Table.descendingSortIcon", type.descendingIcon)
      table.getTableHeader().repaint()
    }
  }
  val box = Box.createHorizontalBox()
  box.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5))
  box.add(JLabel("Table Sort Icon: "))
  SortIconType.entries.forEach { type ->
    val name = type.name
    val selected = type == SortIconType.DEFAULT
    val radio = JRadioButton(name, selected)
    radio.addItemListener(handler)
    radio.actionCommand = name
    box.add(radio)
    group.add(radio)
  }
  box.add(Box.createHorizontalGlue())
  return box
}

private enum class SortIconType {
  DEFAULT,
  EMPTY,
  CUSTOM,
  ;

  val ascendingIcon: Icon
    get() = when (this) {
      DEFAULT -> {
        UIManager
          .getLookAndFeelDefaults()
          .getIcon("Table.ascendingSortIcon")
      }

      EMPTY -> {
        EmptyIcon()
      }

      CUSTOM -> {
        createIcon("example/ascending.png")
      }
    }

  val descendingIcon: Icon
    get() = when (this) {
      DEFAULT -> {
        UIManager
          .getLookAndFeelDefaults()
          .getIcon("Table.descendingSortIcon")
      }

      EMPTY -> {
        EmptyIcon()
      }

      CUSTOM -> {
        createIcon("example/descending.png")
      }
    }

  companion object {
    private fun createIcon(path: String): Icon {
      val url = Thread.currentThread().contextClassLoader.getResource(path)
      val image = url?.openStream()?.use(ImageIO::read) ?: createMissingImage()
      return IconUIResource(ImageIcon(image))
    }

    private fun createMissingImage(): Image {
      val missingIcon = UIManager.getIcon("html.missingImage")
      val iw = missingIcon.iconWidth
      val ih = missingIcon.iconHeight
      val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
      val g2 = bi.createGraphics()
      missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
      g2.dispose()
      return bi
    }
  }
}

private class EmptyIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // Empty icon
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
