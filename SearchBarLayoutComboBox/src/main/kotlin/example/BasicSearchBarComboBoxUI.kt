package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class BasicSearchBarComboBoxUI : SearchBarComboBoxUI() {
  private var popupMenuListener: PopupMenuListener? = null
  private var loupeButton: JButton? = null
  private val loupeAction = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      comboBox.setPopupVisible(false)
      val o = listBox.getSelectedValue() ?: comboBox.getItemAt(0)
      println("$o: ${comboBox.getEditor().getItem()}")
    }
  }

  override fun installDefaults() {
    super.installDefaults()
    // comboBox.setEditable(true)
    comboBox.putClientProperty("JComboBox.isTableCellEditor", true)
    // comboBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT)
  }

  override fun installListeners() {
    super.installListeners()
    popupMenuListener = createPopupMenuListener()
    comboBox.addPopupMenuListener(popupMenuListener)
  }

  override fun uninstallListeners() {
    super.uninstallListeners()
    comboBox.removePopupMenuListener(popupMenuListener)
  }

  private fun createPopupMenuListener(): PopupMenuListener? {
    if (popupMenuListener == null) {
      popupMenuListener = object : PopupMenuListener {
        private var str: String? = null
        override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
          (e.source as? JComboBox<*>)?.also {
            str = it.editor.item.toString()
          }
        }

        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
          val combo = e.source
          val se = listBox.getSelectedValue()
          if (combo is JComboBox<*> && se is SearchEngine) {
            arrowButton.setIcon(se.favicon)
            arrowButton.setRolloverIcon(makeRolloverIcon(se.favicon))
            combo.editor.item = str
          }
        }

        override fun popupMenuCanceled(e: PopupMenuEvent) {
          /* not needed */
        }
      }
    }
    return popupMenuListener
  }

  override fun configureEditor() {
    // super.configureEditor()
    // Should be in the same state as the comboBox
    editor.setEnabled(comboBox.isEnabled)
    editor.setFocusable(comboBox.isFocusable)
    editor.setFont(comboBox.font)
    // editor.addFocusListener(getHandler())
    // comboBox.getEditor().addActionListener(getHandler())
    (editor as? JComponent)?.also {
      // it.putClientProperty("doNotCancelPopup", HIDE_POPUP_KEY)
      it.inheritsPopupMenu = true
      it.border = BorderFactory.createEmptyBorder(0, 4, 0, 0)
      it.actionMap.put("loupe", loupeAction)
      val im = it.getInputMap(JComponent.WHEN_FOCUSED)
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "loupe")
    }
    comboBox.configureEditor(comboBox.getEditor(), comboBox.getSelectedItem())
    editor.addPropertyChangeListener(propertyChangeListener)
  }

  override fun createArrowButton() = TriangleArrowButton()

  override fun configureArrowButton() {
    super.configureArrowButton()
    arrowButton.also {
      it.setBackground(UIManager.getColor("Panel.background"))
      it.setHorizontalAlignment(SwingConstants.LEFT)
      it.setOpaque(true)
      it.setFocusPainted(false)
      it.setContentAreaFilled(false)
      val border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, Color(0x7F_9D_B9)),
        BorderFactory.createEmptyBorder(1, 1, 1, 1)
      )
      it.setBorder(border)
    }
  }

  override fun installComponents() {
    // super.installComponents()
    arrowButton = createArrowButton()
    comboBox.add(arrowButton)
    configureArrowButton()

    loupeButton = createLoupeButton()
    comboBox.add(loupeButton)
    configureLoupeButton()

    // if (comboBox.isEditable())
    addEditor()
    comboBox.add(currentValuePane)
  }

  override fun uninstallComponents() {
    if (loupeButton != null) {
      unconfigureLoupeButton()
    }
    loupeButton = null
    super.uninstallComponents()
  }

  private fun createLoupeButton(): JButton {
    val button = JButton(loupeAction)
    val cl = Thread.currentThread().contextClassLoader
    val url = cl.getResource("example/loupe.png")
    val loupe = if (url == null) {
      UIManager.getIcon("html.missingImage")
    } else {
      ImageIcon(url)
    }
    button.icon = loupe
    button.rolloverIcon = makeRolloverIcon(loupe)
    return button
  }

  private fun configureLoupeButton() {
    loupeButton?.also {
      it.name = "ComboBox.loupeButton"
      it.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
      it.isEnabled = comboBox.isEnabled()
      it.isFocusable = comboBox.isFocusable()
      it.isOpaque = false
      it.isRequestFocusEnabled = false
      it.isFocusPainted = false
      it.isContentAreaFilled = false
      // it.addMouseListener(popup.getMouseListener())
      // it.addMouseMotionListener(popup.getMouseMotionListener())
      it.resetKeyboardActions()
      // it.putClientProperty("doNotCancelPopup", HIDE_POPUP_KEY)
      it.inheritsPopupMenu = true
    }
  }

  private fun unconfigureLoupeButton() {
    loupeButton?.action = null
  }

  override fun createRenderer() = SearchEngineListCellRenderer<Any>()

  override fun createLayoutManager() = SearchBarLayout()

  private fun makeRolloverIcon(srcIcon: Icon): Icon {
    val op = RescaleOp(floatArrayOf(1.2f, 1.2f, 1.2f, 1f), floatArrayOf(0f, 0f, 0f, 0f), null)
    val img = BufferedImage(srcIcon.iconWidth, srcIcon.iconHeight, BufferedImage.TYPE_INT_ARGB)
    val g = img.graphics
    // g.drawImage(srcIcon.getImage(), 0, 0, null)
    srcIcon.paintIcon(null, g, 0, 0)
    g.dispose()
    return ImageIcon(op.filter(img, null))
  }

//  companion object {
//    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
//    //   the same signature as a static method in a Java base class : KT-12993
//    // https://youtrack.jetbrains.com/issue/KT-12993
//    @JvmStatic
//    fun createUI(c: JComponent): ComponentUI {
//      return BasicSearchBarComboBoxUI()
//    }
//  }
}
