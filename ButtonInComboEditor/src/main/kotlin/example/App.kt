package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val image1 = ImageIcon(javaClass.getResource("favicon.png"))
    val image2 = ImageIcon(javaClass.getResource("16x16.png"))
    val rss = ImageIcon(javaClass.getResource("feed-icon-14x14.png")) // http://feedicons.com/

    val combo01 = JComboBox<SiteItem>(makeModel(image1, image2))
    initComboBox(combo01)

    val combo02 = SiteItemComboBox(makeModel(image1, image2), rss)
    initComboBox(combo02)

    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createTitledBorder("setEditable(true)"))
    box.add(Box.createVerticalStrut(2))
    box.add(combo01)
    box.add(Box.createVerticalStrut(5))
    box.add(combo02)
    box.add(Box.createVerticalStrut(2))

    add(box, BorderLayout.NORTH)
    add(JScrollPane(JTextArea()))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeModel(i1: Icon, i2: Icon) = DefaultComboBoxModel<SiteItem>().also {
    it.addElement(SiteItem("https://ateraimemo.com/", i1, true))
    it.addElement(SiteItem("https://ateraimemo.com/Swing.html", i1, true))
    it.addElement(SiteItem("https://ateraimemo.com/Kotlin.html", i1, true))
    it.addElement(SiteItem("https://github.com/aterai/java-swing-tips", i2, true))
    it.addElement(SiteItem("https://java-swing-tips.blogspot.com/", i2, true))
    it.addElement(SiteItem("http://www.example.com/", i2, false))
  }

  private fun initComboBox(combo: JComboBox<SiteItem>) {
    combo.setEditable(true)
    combo.setRenderer(object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        c.setIcon((value as? SiteItem)?.favicon)
        return c
      }
    })
  }
}

class SiteItemComboBox(model: DefaultComboBoxModel<SiteItem>, rss: ImageIcon) : JComboBox<SiteItem>(model) {
  init {
    val field = getEditor().getEditorComponent() as JTextField
    val feedButton = makeRssButton(rss)
    val favicon = makeLabel(field)
    setLayout(SiteComboBoxLayout(favicon, feedButton))
    add(feedButton)
    add(favicon)

    field.addFocusListener(object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        // field.setBorder(BorderFactory.createEmptyBorder(0, 16 + 4, 0, 0));
        feedButton.setVisible(false)
      }

      override fun focusLost(e: FocusEvent) {
        getSiteItemFromModel(model, field.getText())?.also { item ->
          model.removeElement(item)
          model.insertElementAt(item, 0)
          favicon.setIcon(item.favicon)
          feedButton.setVisible(item.hasRss)
          setSelectedIndex(0)
        }
      }
    })
    addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateFavicon(model, favicon)
      }
    }
    updateFavicon(model, favicon)
  }

  private fun updateFavicon(model: ComboBoxModel<SiteItem>, label: JLabel) {
    EventQueue.invokeLater {
      getSiteItemFromModel(model, getSelectedItem())?.also { label.setIcon(it.favicon) }
    }
  }

  private fun makeRssButton(rss: ImageIcon) = JButton(rss).also {
    val ip = FilteredImageSource(rss.getImage().getSource(), SelectedImageFilter())
    it.setRolloverIcon(ImageIcon(Toolkit.getDefaultToolkit().createImage(ip)))
    // it.setRolloverIcon(makeFilteredImage(rss));
    // it.setRolloverIcon(makeFilteredImage2(rss));
    it.addActionListener { println("clicked...") }
    it.setFocusPainted(false)
    it.setBorderPainted(false)
    it.setContentAreaFilled(false)
    it.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
    it.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 2))
  }

  private fun makeLabel(field: JTextField) = JLabel().also {
    it.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        EventQueue.invokeLater {
          field.requestFocusInWindow()
          field.selectAll()
        }
      }
    })
    it.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
    it.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 2))
  }

  protected fun getSiteItemFromModel(model: ComboBoxModel<SiteItem>, o: Any?): SiteItem? {
    if (o is SiteItem) {
      return o
    }
    val str = o?.toString() ?: ""
    return (0 until model.getSize()).map { model.getElementAt(it) }.filter { it.url == str }.firstOrNull()
  }
}

internal class SiteComboBoxLayout(private val favicon: JLabel?, private val feedButton: JButton?) : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component) {}

  override fun removeLayoutComponent(comp: Component) {}

  override fun preferredLayoutSize(parent: Container) = parent.getPreferredSize()

  override fun minimumLayoutSize(parent: Container) = parent.getMinimumSize()

  override fun layoutContainer(parent: Container) {
    val cb = parent as? JComboBox<*> ?: return
    val width = cb.getWidth()
    val height = cb.getHeight()
    val ins = cb.getInsets()
    val arrowHeight = height - ins.top - ins.bottom
    var arrowWidth = arrowHeight
    var faviconWidth = arrowHeight
    var feedWidth = 0

    // Arrow Icon JButton
    (cb.getComponent(0) as? JButton)?.also {
      val arrowInsets = it.getInsets()
      arrowWidth = it.getPreferredSize().width + arrowInsets.left + arrowInsets.right
      it.setBounds(width - ins.right - arrowWidth, ins.top, arrowWidth, arrowHeight)
    }

    // Favicon JLabel
    favicon?.also {
      val faviconInsets = it.getInsets()
      faviconWidth = it.getPreferredSize().width + faviconInsets.left + faviconInsets.right
      it.setBounds(ins.left, ins.top, faviconWidth, arrowHeight)
    }

    // Feed Icon JButton
    feedButton?.takeIf { it.isVisible() }?.also {
      val feedInsets = it.getInsets()
      feedWidth = it.getPreferredSize().width + feedInsets.left + feedInsets.right
      it.setBounds(width - ins.right - feedWidth - arrowWidth, ins.top, feedWidth, arrowHeight)
    }

    // JComboBox Editor
    cb.getEditor().getEditorComponent()?.also {
      it.setBounds(ins.left + faviconWidth, ins.top,
          width - ins.left - ins.right - arrowWidth - faviconWidth - feedWidth, height - ins.top - ins.bottom)
    }
  }
}

internal class SiteItem(val url: String, val favicon: Icon, val hasRss: Boolean) {
  override fun toString() = url
}

internal class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = Math.min(0xFF, ((argb shr 16 and 0xFF) * SCALE).toInt())
    val g = Math.min(0xFF, ((argb shr 8 and 0xFF) * SCALE).toInt())
    val b = Math.min(0xFF, ((argb and 0xFF) * SCALE).toInt())
    // return argb and -0x1000000 or (r shl 16) or (g shl 8) or b
    return argb and 0xFF_00_00_00.toInt() or (r shl 16) or (g shl 8) or b
  }

  companion object {
    private const val SCALE = 1.2f
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
