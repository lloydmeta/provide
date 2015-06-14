package com.beachape.annotations

import scala.annotation.{ compileTimeOnly, StaticAnnotation }
import scala.language.experimental.macros

/**
 * Created by Lloyd on 6/13/15.
 */

/**
 * Add this annotation to make sure that you are implementing a method that
 * exists as an abstract or abstract override method in parent classes/traits
 */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class provide extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro ProvideMacro.provideImp

}
