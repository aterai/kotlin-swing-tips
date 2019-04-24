package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

open class SearchEngine(val name: String, val url: String, val favicon: ImageIcon) {
  override fun toString() = name
}
